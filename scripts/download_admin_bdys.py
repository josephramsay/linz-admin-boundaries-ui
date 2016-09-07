#!/usr/bin/env python
################################################################################
#
# download_admin_bdys.py
#
# Copyright 2014 Crown copyright (c)
# Land Information New Zealand and the New Zealand Government.
# All rights reserved
#
# This program is released under the terms of the new BSD license. See the 
# LICENSE file for more information.
#
################################################################################
# Notes
# to fetch localities file need share created to \\prdassfps01\GISData\Electoral specific\Enrollment Services\Meshblock_Address_Report
# to fetch meshblock data need sftp connection to 144.66.244.17/Meshblock_Custodianship 
# without updated python >2.7.9 cant use paramiko (see commit history) use pexpect instead
# database conn uses lds_bde user and modifed pg_hba allowing; local, lds_bde, linz_db, peer 

# TODO
# No | X | Desc
# ---+---+-----
# 1. |   | Change legacy database config to common attribute mapping
# 2. |   | Shift file to table mapping into config
# 3. | x | Enforce create/drop schema
# 4. |   | Consistent return types from db calls
# 5. |   | Validation framework
 
__version__ = 1.0

import os
import sys
import re
import json
import string
import socket
import logging.config
import getopt
import psycopg2


#2 to 3 imports
try:
    import Tkinter as TK
    from Tkconstants import RAISED,SUNKEN,BOTTOM,RIGHT,LEFT,END,X,Y,W,E,N,S,ACTIVE  
    from ConfigParser import SafeConfigParser
except ImportError:
    import tkinter as TK
    from tkinter.constants import RAISED,SUNKEN,BOTTOM,RIGHT,LEFT,END,X,Y,W,E,N,S,ACTIVE  
    from configparser import SafeConfigParser

import socket,time
from zipfile import ZipFile

#from paramiko import Transport, SFTPClient

# from twisted.internet import reactor
# from twisted.internet.defer import Deferred
# from twisted.conch.ssh.common import NS
# from twisted.conch.scripts.cftp import ClientOptions
# from twisted.conch.ssh.filetransfer import FileTransferClient
# from twisted.conch.client.connect import connect
# from twisted.conch.client.default import SSHUserAuthClient, verifyHostKey
# from twisted.conch.ssh.connection import SSHConnection
# from twisted.conch.ssh.channel import SSHChannel
# from twisted.python.log import startLogging, err

from subprocess import Popen,PIPE,check_output

import pexpect


from optparse import OptionParser

try:
    from osgeo import ogr, osr, gdal
except:
    try:
        import ogr, osr, gdal
    except Exception as e:
        raise Exception('ERROR: cannot find python OGR and GDAL modules'+str(e))
        #sys.exit('ERROR: cannot find python OGR and GDAL modules')

version_num = int(gdal.VersionInfo('VERSION_NUM'))
if version_num < 1100000:
    raise Exception('ERROR: Python bindings of GDAL 1.10 or later required')
    #sys.exit('ERROR: Python bindings of GDAL 1.10 or later required')

# make sure gdal exceptions are not silent
gdal.UseExceptions()
osr.UseExceptions()
ogr.UseExceptions()

logger = None

# Prefix for imported temp tables
PREFIX = 'temp_'
# Prefix for snapshot tables
SNAP = 'snap_'
# Use the temp schema to create snapshots to test against (won't overwrite admin_bdys tables)
TEST = True
# Holds dataabse connection instances
SELECTION = {'ogr':None,'psy':None}
# Number of query attempts to make
DEPTH = 5
#Processing options
OPTS = [('Load','load',0),('Map','map',1),('Transfer','transfer',2),('Reject','reject',3)]

def shift_geom ( geom ):
    '''translate geometry to 0-360 longitude space'''
    if geom is None:
        return
    count = geom.GetGeometryCount()
    if count > 0:
        for i in range( count ):
            shift_geom( geom.GetGeometryRef( i ) )
    else:
        for i in range( geom.GetPointCount() ):
            x, y, z = geom.GetPoint( i )
            if x < 0:
                x = x + 360
            elif x > 360:
                x = x - 360
            geom.SetPoint( i, x, y, z )
    return

def ring_is_clockwise(ring):
    '''check is geometry ring is clockwise'''
    total = 0
    i = 0
    point_count = ring.GetPointCount()
    pt1 = ring.GetPoint(i)
    pt2 = None
    for i in range(point_count-1):
        pt2 = ring.GetPoint(i+1)
        total += (pt2[0] - pt1[0]) * (pt2[1] + pt1[1])
        pt1 = pt2
    return (total >= 0)

def fix_esri_polyon(geom):
    '''this is required because of a bug in OGR http://trac.osgeo.org/gdal/ticket/5538'''
    polygons = []
    count = geom.GetGeometryCount()
    if count > 0:
        poly = None
        for i in range( count ):
            ring = geom.GetGeometryRef(i)
            if ring_is_clockwise(ring):
                poly = ogr.Geometry(ogr.wkbPolygon)
                poly.AddGeometry(ring)
                polygons.append(poly)
            else:
                poly.AddGeometry(ring)
    new_geom = None
    if  len(polygons) > 1:
        new_geom = ogr.Geometry(ogr.wkbMultiPolygon)
        for poly in polygons:
            new_geom.AddGeometry(poly)
    else:
        new_geom = polygons.pop()
    return new_geom

class DataValidator(object):
    #DRAFT
    def __init__(self,conf):
        self.conf = conf
        
    def validateSpatial(self):
        '''Validates using specific queries, spatial or otherwise eg select addressPointsWithinMeshblocks()'''
        for f in self.conf.validation_spatial:
            Processor.attempt(self.conf, f)
            
    def validateData(self):
        '''Validates the ref data itself, eg enforcing meshblock code length'''
        for f in self.conf.validation_data:
            Processor.attempt(self.conf, f)

class ColumnMapperError(Exception):pass
class ColumnMapper(object):
    '''Acions the list of column mappings from conf file'''
    map = {}
    dra = {'drop':'ALTER TABLE {schema}.{table} DROP COLUMN IF EXISTS {drop}',
           'rename':'ALTER TABLE {schema}.{table} RENAME COLUMN {old} TO {new}',
           'add':'ALTER TABLE {schema}.{table} ADD COLUMN {add} {type}',
           'cast':'ALTER TABLE {schema}.{table} ALTER COLUMN {cast} SET DATA TYPE {type}'
    }
    
    def __init__(self,conf):
        self.schema = conf.db_schema
        for attr in conf.__dict__:
            m = re.search('(\w+)_colmap',attr)
            if m: self.map[m.group(1)] = json.loads(getattr(conf,attr))
            
    def action(self,section,tablename,action):
        '''Generate queries from the column map'''
        _test = self.map.has_key(section) and self.map[section].has_key(tablename) and self.map[section][tablename].has_key(action)
        return [self.formqry(action,PREFIX+tablename, sta) for sta in self.map[section][tablename][action]] if _test else []
    
    def _getArgs(self,a):
        return a.values() if type(a) in (dict,) else a
        
    def formqry(self,action,table,args):
        #print action, table, args
        if action == 'drop': return self.dra[action].format(schema=self.schema,table=table,drop=args)
        elif action == 'rename': return self.dra[action].format(schema=self.schema,table=table,old=args['old'],new=args['new'])
        elif action == 'add': return self.dra[action].format(schema=self.schema,table=table,add=args['add'],type=args['type'])
        elif action == 'cast': return self.dra[action].format(schema=self.schema,table=table,cast=args['cast'],type=args['type'])
        raise ColumnMapperError('Unrecognised query type specifier, use drop/add/rename/cast')    
    
    def _formqry(self,f,d):
        #print f,d
        #print f.format(*d)
        return f.format(*d)
        
class DBSelectionException(Exception):pass
class DB(object):
    def __init__(self,conf,drv):
        self.conf = conf
        if drv == 'ogr':
            self.d = DatabaseConn_ogr(self.conf)
            self.d.connect()
        elif drv == 'psy':
            self.d = DatabaseConn_psycopg2(self.conf)
            self.d.connect()
        else:raise DBSelectionException("Choose DB using 'ogr' or 'psy'")

    def get(self,q):
        return bool(self.d.execute(q))
        
    def __enter__(self):
        return self
    
    def __exit__(self,exc_type=None, exc_val=None, exc_tb=None):
        self.d.disconnect()
            
class DatabaseConnectionException(Exception):pass
class DatabaseConn_psycopg2(object):
    def __init__(self,conf):
        self.conf = conf
        self.exe = None
        
    def connect(self):
        self.pconn = psycopg2.connect( \
            host=self.conf.database_host,\
            database=self.conf.database_name,\
            user=self.conf.database_user,\
            password=self.conf.database_password)
        self.pconn.set_isolation_level(psycopg2.extensions.ISOLATION_LEVEL_AUTOCOMMIT)
        self.pcur = self.pconn.cursor()
        
    def execute(self,q):
        '''Execute query q and return success/failure determined by fail=any error except no results'''
        res = True
        try:
            self.pcur.execute(q)
            res = self.pcur.rowcount or None
        except psycopg2.ProgrammingError as pe: 
            if pe.message=='no results to fetch': res = True
            else: raise 
        except Exception as e: 
            raise DatabaseConnectionException('Database query error, {}'.format(e))
        #for now just check if row returned as success
        return bool(res)
    
    def disconnect(self):
        self.pconn.commit()
        self.pcur.close()
        self.pconn.close()

class DatabaseConn_ogr(object):
    
    def __init__(self,conf):
        
        self.conf = conf
        self.exe = None
        self.pg_drv = ogr.GetDriverByName('PostgreSQL')
        if self.pg_drv is None:
            logger.fatal('Could not load the OGR PostgreSQL driver')
            raise Exception('Could not load the OGR PostgreSQL driver')
            #sys.exit(1)
        
        self.pg_uri = 'PG:dbname=' + conf.db_name
        if conf.db_host:
            self.pg_uri = self.pg_uri + ' host=' +  conf.db_host
        if conf.db_port:
            self.pg_uri = self.pg_uri + ' port=' +  conf.db_port
        if conf.db_user:
            self.pg_uri = self.pg_uri + ' user=' +  conf.db_user
        if conf.db_pass:
            self.pg_uri = self.pg_uri + ' password=' +  conf.db_pass        
        if conf.db_schema:
            pass
            #self.pg_uri = self.pg_uri + ' active_schema=' +  conf.db_schema
        
        self.pg_ds = None
        
    def connect(self):
        try:
            if not self.pg_ds: 
                self.pg_ds = self.pg_drv.Open(self.pg_uri, update = 1)
                if self.conf.db_rolename:
                    self.pg_ds.ExecuteSQL("SET ROLE " + self.conf.db_rolename)
        except Exception as e:
            logger.fatal("Can't open PG output database: " + str(e))
            raise
            #sys.exit(1)
            
    def execute(self,q):
        return self.pg_ds.ExecuteSQL(q)
           
    def disconnect(self):
        del self.pg_ds
                   
class ConfReader(object):
    TEMP = 'temp'
    
    def __init__(self):
        #legacy, can remove most of this!
        global logger
        usage = "usage: %prog config_file.ini"
        oparser = OptionParser(usage=usage)
        (cmd_opt, args) = oparser.parse_args()
           
        #if len(args) == 1:
        #    config_files = [args[0]]
        #else:
        self.config_files = ['download_admin_bdys.ini']
        
        self.parser = SafeConfigParser()
        found = self.parser.read(self.config_files)
        if not found:
            raise Exception('Could not load config ' + self.config_files[0] )
            #sys.exit('Could not load config ' + config_files[0] )
        
        # set up logging
        logging.config.fileConfig(self.config_files[0], defaults={ 'hostname': socket.gethostname() })
        logger = logging.getLogger()
        
        self.db_host = None
        self.db_rolename = None
        self.db_port = None
        self.db_user = None
        self.db_pass = None
        self.db_schema = 'public'
        self.layer_name = None
        self.layer_geom_column = None
        self.layer_output_srid = 4167
        self.create_grid = False
        self.grid_res = 0.05
        self.shift_geometry = False
        
        self.base_uri = self.parser.get('source', 'base_uri')
        self.db_name = self.parser.get('database', 'name')
        self.db_schema = self.parser.get('database', 'schema')
        
        if self.parser.has_option('database', 'rolename'):
            self.db_rolename = self.parser.get('database', 'rolename')
        if self.parser.has_option('database', 'host'):
            self.db_host = self.parser.get('database', 'host')
        if self.parser.has_option('database', 'port'):
            self.db_port = self.parser.get('database', 'port')
        if self.parser.has_option('database', 'user'):
            self.db_user = self.parser.get('database', 'user')
        if self.parser.has_option('database', 'password'):
            self.db_pass = self.parser.get('database', 'password')
            
        self.layer_name = self.parser.get('layer', 'name')
        self.layer_geom_column = self.parser.get('layer', 'geom_column')
        if self.parser.has_option('layer', 'output_srid'):
            self.layer_output_srid = self.parser.getint('layer', 'output_srid')
        if self.parser.has_option('layer', 'create_grid'):
            self.create_grid = self.parser.getboolean('layer', 'create_grid')
        if self.parser.has_option('layer', 'grid_res'):
            self.grid_res = self.parser.getfloat('layer', 'grid_res')
        if self.parser.has_option('layer', 'shift_geometry'):
            self.shift_geometry = self.parser.getboolean('layer', 'shift_geometry')
            
        #meshblocks
        for section in ('connection','meshblock','nzlocalities','database'):
            for option in self.parser.options(section): 
                setattr(self,'{}_{}'.format(section,option),self.parser.get(section,option))
            
        # set up logging
        logging.config.fileConfig(self.config_files[0], defaults={ 'hostname': socket.gethostname() })
        logger = logging.getLogger()
    
        logger.info('Starting download TA boundaries')
        
    def save(self,name,data):
        '''configparser save for interrupted processing jobs'''
        if not self.parser.has_section(self.TEMP): 
            self.parser.add_section(self.TEMP)
        self.parser.set(self.TEMP,name,json.dumps(data))
        with open(self.config_files[0], 'wb') as configfile: self.parser.write(configfile)
        
    def read(self,name):
        '''configparser read for interrupted processing jobs'''
        rv = ()
        if self.parser.has_section(self.TEMP) and self.parser.has_option(self.TEMP,name): 
            rv = json.loads(self.parser.get(self.TEMP,name))
            #if clean is set the data will be deleted after this read so delete the section/option to prevent attempted reread
            self.parser.remove_option(self.TEMP,name)
            self.parser.remove_section(self.TEMP)
            with open(self.config_files[0], 'wb') as configfile: self.parser.write(configfile)
        return rv
        
    
class ProcessorException(Exception):pass
class Processor(object):
    mbcc = ('OBJECTID','Meshblock','TA','TA Ward','Community Board','TA Subdivision','TA Maori_Ward','Region', \
            'Region Constituency','Region Maori Constituency','DHB','DHB Constituency','GED 2007','MED 2007', \
            'High Court','District Court','GED','MED','Licensing Trust Ward')
    #filename to table+column name translations
    f2t = {'Stats_MB_WKT.csv':['meshblock','<todo create columns>'], \
           'Stats_Meshblock_concordance.csv':['meshblock_concordance',mbcc], \
           'Stats_Meshblock_concordance_WKT.csv':['meshblock_concordance',mbcc], \
           'Stats_TA_WKT.csv':['territorial_authority','<todo create columns>']}
           
    #mapping for csv|shapefilenames to tablenames
    #{shapefile:[import tablename, original tablename]}
    l2t = {'nz_localities':['nz_locality','nz_locality'],
           'StatsNZ_Meshblock':['statsnz_meshblock','meshblock'],
           'StatsNZ_TA':['statsnz_ta','territorial_authority'],
           'Stats_Meshblock_concordance':['meshblock_concordance','meshblock_concordance']}
    
    q = {'find':"select count(*) from information_schema.tables where table_schema like '{}' and table_name = '{}'",
         'create':'create table {}.{} ({})',
         'insert':'insert into {}.{} ({}) values ({})',
         'trunc':'truncate table {}.{}',
         'drop':'drop table if exists {}.{}'}
    
    enc = 'utf-8-sig'
    
    def __init__(self,conf,db,cm,sf):
        self.conf = conf
        self.db = db
        self.cm = cm
        self.driver = ogr.GetDriverByName('ESRI Shapefile')
        self.sftp = sf
        self.secname = type(self).__name__.lower()

    def extract(self,file):
        '''Takes a zip path/filename input and returns the path/names of any unzipped files'''
        nl = []
        with ZipFile(file,'r') as h:
            nl = h.namelist()
            for fname in nl:
                h.extract(fname)
        return ['{}/{}'.format(getattr(self.conf,'{}_localpath'.format(self.secname)),n) for n in nl] 
    
    @classmethod
    def recent(cls,filelist,pattern='[a-zA-Z_]*(\d{8}).*'):
        '''get the latest date labelled file from a list'''
        extract = {re.match(pattern,val).group(1):val for val in filelist if re.match(pattern,val)} 
        return extract[max(extract)]
        
    def delete(self,file):
        '''clean up unzipped shapefile'''
        #for file in path:
        p,f = os.path.split(file)
        ff,fx = os.path.splitext(f)
        for candidate in os.listdir(p):
            cf,cx = os.path.splitext(candidate)
            if re.match(ff,cf): os.remove(os.path.join(p,candidate))
            
    def query(self,schema,table,headers='',values='',op='insert'):
        h = ','.join([i.replace(' ','_') for i in headers]).lower() if hasattr(headers,'__iter__') else headers
        v = ','.join(values) if hasattr(values,'__iter__') else values
        return self.q[op].format(schema,table,h, v).replace('"','\'')
    
    def layername(self,in_layer):
        '''Returns the name of the layer that inserting a shapefile would create'''
        in_name = in_layer.GetName()
        return self.l2t[in_name][0] if self.l2t.has_key(in_name) else in_name
        
    def deletelyr(self,tname):
        #dlayer = self.db.pg_ds.GetLayerByName('{}.{}'.format(self.conf.db_schema,tname))
        #self.db.pg_ds.DeleteLayer(dlayer.GetName())
        self.db.pg_ds.DeleteLayer('{}.{}{}'.format(self.conf.db_schema,PREFIX,tname))
        
    def insertshp(self,in_layer):
        if not in_layer: raise ProcessorException('Attempt to process Empty Datasource')
        in_name,out_name = None,None

        #options
        create_opts = ['GEOMETRY_NAME='+'geom']
        create_opts.append('SCHEMA=' + self.conf.db_schema)
        create_opts.append('OVERWRITE=' + 'yes')
        
        #create new layer
        try: 
            in_name = in_layer.GetName()
            logger.info('Inserting shapefile {}'.format(in_name))
            out_name = PREFIX+self.l2t[in_name][0] if self.l2t.has_key(in_name) else in_name

            out_srs = in_layer.GetSpatialRef()
            out_layer = self.db.pg_ds.CreateLayer(
                out_name,
                srs = out_srs,
                geom_type = ogr.wkbMultiPolygon,
                options = create_opts
            )
            #build layer fields
            in_ldef = in_layer.GetLayerDefn()
            for i in range(0, in_ldef.GetFieldCount()):
                in_fdef = in_ldef.GetFieldDefn(i)
                out_layer.CreateField(in_fdef)
        except RuntimeError as r:
            #Version.rebuild(self.conf) If a problem occurs any previously created tables are deleted
            logger.warn('Error creating layer {}, drop and rebuild. {}'.format(out_name,r))
            q1 = 'drop table if exists {} cascade'.format(out_name)
            Processor.attempt(self.conf, q1, select='psy')
            return self.insertshp(in_layer)
        except Exception as e:
            logger.fatal('Can not create {} output table {}'.format(out_name,e))
            raise
            #sys.exit(1)
            
        #insert features
        try:
            in_layer.ResetReading()
            in_feat = in_layer.GetNextFeature()
            out_ldef = out_layer.GetLayerDefn()
            while in_feat:
                out_feat = ogr.Feature(out_ldef)
                for i in range(0, out_ldef.GetFieldCount()):
                    out_feat.SetField(out_ldef.GetFieldDefn(i).GetNameRef(), in_feat.GetField(i))
                geom = in_feat.GetGeometryRef()
                #1. fix_esri_polygon (no longer needed?)
                #geom = fix_esri_polyon(geom)
                #2. shift_geom
                if out_srs.IsGeographic() and self.conf.shift_geometry:
                        shift_geom(geom)
                #3. always force, bugfix
                geom = ogr.ForceToMultiPolygon(geom)
                out_feat.SetGeometry(geom)
                out_layer.CreateFeature(out_feat)
                in_feat = in_layer.GetNextFeature()
            
        except Exception as e:
            logger.fatal('Can not populate {} output table {}'.format(e))
            raise 
            #sys.exit(1)
            
        return out_name
            
    def insertcsv(self,mbfile):
        fp,ff = os.path.split(mbfile) 
        logger.info('Inserting csv {}'.format(ff))
        #self.db.connect()
        #mb = '/home/jramsay/Downloads/Stats_MB_TA_WKT_20160415-NEW.zip'
        first = True
        # this is a hack while using temptables
        csvhead = self.f2t[ff]
        with open(ff,'r') as fh:
            for line in fh:
                line = line.strip().decode(self.enc)#.replace('"','\'')
                if first: 
                    headers = line.split(',')
                    findqry = self.query(self.conf.db_schema,PREFIX+csvhead[0],op='find')
                    if self.execute(findqry).GetNextFeature().GetFieldAsInteger(0) == 0:
                        storedheaders = ','.join(['{} VARCHAR'.format(m.replace(' ','_')) for m in csvhead[1]])
                        createqry = self.query(self.conf.db_schema,PREFIX+csvhead[0],storedheaders,op='create')
                        self.execute(createqry)
                    else:
                        truncqry = self.query(self.conf.db_schema,PREFIX+csvhead[0],op='trunc')
                        self.execute(truncqry)
                    first = False
                else:
                    values = line.replace("'","''").split(',',len(headers)-1)
                    #if int(values[0])<47800:continue
                    if '"NULL"' in values: continue
                    insertqry = self.query(self.conf.db_schema,PREFIX+csvhead[0],headers,values,op='insert')
                    self.execute(insertqry)
        #self.db.disconnect()            
        return csvhead[0]
                           
    def mapcolumns(self,tablename):
        '''Perform input to final column mapping'''
        actions = ('add','drop','rename','cast')
        for qlist in [self.cm.action(self.secname,tablename.lower(),adrc) for adrc in actions]: 
            for q in qlist: 
                if q: self.execute(q)
                
    def drop(self,table):
        '''Clean up any previous table instances. Doesn't work!''' 
        return self.execute(self.q['drop'].format(self.conf.db_schema,table))


    @staticmethod
    def attempt(conf,q,select='ogr',depth=DEPTH):
        '''Attempt connection using ogr or psycopg drivers creating temp connection if conn object not stored'''
        m,r = None,None
        while depth>0:
            try:
                #if using active DB instance 
                if SELECTION[select]:
                    m = SELECTION[select].execute(q)
                    return m
                #otherwise setup/delete temporary connection
                else:
                    with DB(conf,select) as conn:
                        m = conn.get(q)
                        return m
            except RuntimeError as r:
                print ('Attempt {} using {} failed, {}'.format(DEPTH-depth+1,select,m or r))
                #if re.search('table_version.ver_apply_table_differences',q) and Processor.nonOGR(conf,q,depth-1): return
                return Processor.attempt(conf, q, Processor._next(select), depth-1)
        if r: raise r
        
    @staticmethod
    def _next(s,slist=None):
        slist = slist or SELECTION
        return slist.keys()[(slist.keys().index(s)+1)%len(slist)]
                              
    def execute(self,q):  
        try:
            #logger.info('Executing SQL {}'.format(q))
            #return self.db.pg_ds.ExecuteSQL(q)
            return Processor.attempt(self.conf, q)
        except Exception as e:
            logger.error('Error executing query {}\n{}'.format(q,e))
    
class Meshblock(Processor):
    '''Extract and process the meshblock, concordance and boundaries layers'''
    
    def __init__(self,conf,db,cm,sf):
        super(Meshblock,self).__init__(conf,db,cm,sf)   
             
    def run(self):
        self.get()
        return self.secname,self.process()
        
    def get(self): 
        dfile = self.sftp.fetch(self.secname)
        #dfile='./Stats_Meshblock_concordance_20160607.zip'
        if re.search('\.zip$',dfile): 
            self.file = self.extract(dfile)
        else: self.file = [dfile,]
        
    def process(self,pathlist=None):
        tlist = ()
        #self.db.connect()
        ds = None
        if not pathlist: pathlist = [f for f in self.file if re.search('\.csv$|\.shp$',f)]
        #extract the shapefiles
        for mbfile in pathlist:
            #extract the shapefiles
            if re.match('.*\.shp$',mbfile):
                #self.mapcolumns(type(self).__name__.lower(),self.insertshp(self.driver.Open(mbfile,0).GetLayer(0))) #Gives OGR error!!! Assume unreferenced DS is GC'd?
                mbhandle = self.driver.Open(mbfile,0)
                mblayer = mbhandle.GetLayer(0)
                tname = self.layername(mblayer)
                #self.drop(tname) #this doesn't work for some reason
                #self.deletelyr(tname)
                self.insertshp(mblayer)
                self.mapcolumns(tname)
                tlist += (tname,)
                mbhandle.Destroy()
                
            #extract the concordance csv
            elif re.match('.*\.csv$',mbfile):
                tname = self.insertcsv(mbfile)
                self.mapcolumns(tname)
                tlist += (tname,)
            
            self.delete(mbfile)
        #self.db.disconnect()
        return tlist
     
class NZLocalities(Processor):
    '''Exract and process the nz_localities file'''
    #NB new format, see nz_locality
    
    def __init__(self,conf,db,cm,sf):
        super(NZLocalities,self).__init__(conf,db,cm,sf)
        
    def run(self):
        self.get()
        return self.secname,self.process()
        
    def get(self): 
        pass
    
    def process(self,pathlist=None):
        tlist = ()
        #self.db.connect()
        ds = None
        if not pathlist: pathlist = '{}{}.shp'.format(self.conf.nzlocalities_filepath,self.conf.nzlocalities_filename)
        ds = self.driver.Open(pathlist,0)
        if ds:
            nzlayer = ds.GetLayer(0)
            tname = self.layername(nzlayer)
            self.insertshp(nzlayer)
            self.mapcolumns(tname)
            tlist += (tname,)
            ds.Destroy()
        else:
            raise ProcessorException('Unable to initialise data source {}'.format(pathlist))
        #self.db.disconnect()
        return tlist
        
class Version(object):
    
    importfile = 'aimsref_import.sql'
    qtv = 'select table_version.ver_apply_table_differences({}, {}, {})'
    
    def __init__(self,conf,cm):
        self.conf = conf
        self.cm = cm
        
        global SNAP
        if TEST:
            self.qset = self._testquery
            SNAP = 'snap_'
        else:
            self.qset = self._query
            SNAP = ''
        
    def setup(self):
        '''Create temp schema'''
        self.rebuild(self.conf)
               
    @staticmethod     
    def rebuild(conf):              
        #self.db.pg_ds.ExecuteSQL('drop schema if exists {} cascade'.format(self.conf.database_schema))
        #self.db.pg_ds.ExecuteSQL('create schema {}'.format(self.conf.database_schema))          
        q1 = 'drop schema if exists {} cascade'.format(conf.database_schema)
        q2 = 'create schema {}'.format(conf.database_schema)
        Processor.attempt(conf, q1, select='psy')
        Processor.attempt(conf, q2, select='psy')
        
    def teardown(self):
        '''drop temp schema'''
        #self.db.pg_ds.ExecuteSQL('drop schema if exists {}'.format(self.conf.database_schema)) 
        q3 = 'drop schema if exists {} cascade'.format(self.conf.database_schema)
        Processor.attempt(self.conf, q3, select='psy')
        #self.db.disconnect()
        
    def _pktest(self,s,t):
        '''Check whether the table had a primary key already. ExecuteSQL returns layer if successful OR null on error/no-result'''
        q = "select * from information_schema.table_constraints \
             where table_schema like '{s}' \
             and table_name like '{t}' \
             and constraint_type like 'PRIMARY KEY'".format(s=s,t=t)
        print ('pQ2',q)
        #return bool(self.db.pg_ds.ExecuteSQL(q).GetFeatureCount())
        return Processor.attempt(self.conf, q, select='psy')
        
    def _testquery(self,original,snap,imported,pk,geom,srid,final):
        '''Temp setup to create temporary tables without interfering with in-use admin_bdy tables'''
        q = []        
        if final:
            q.append("select table_version.ver_apply_table_differences('{original}','{imported}','{pk}')".format(original=original,imported=imported,pk=pk))
        else:
            si = imported.split('.')
            q.append('create table {snap} as select * from {orig}'.format(snap=snap,orig=original))
            if not self._pktest(si[0],snap.split('.')[1]):
                q.append('alter table {snap} add primary key ({pk})'.format(snap=snap,pk=pk))
            if not self._pktest(si[0],si[1]):
                q.append('alter table {imported} add primary key ({pk})'.format(imported=imported,pk=pk))
            if geom and srid:
                q.append("select UpdateGeometrySRID('{schema}','{imported}', '{geom}', {srid})".format(schema=si[0],imported=si[1],geom=geom,srid=srid))
                q.append("update {imported} set shape = ST_Transform({geom}::geometry,{srid}::integer)".format(imported=imported,geom=geom,srid=srid))
            #q.append("select table_version.ver_apply_table_differences('{snap}','{imported}','{pk}')".format(snap=snap,imported=imported,pk=pk))
            #for i in q: print i
        return q
        
    def _query(self,original,_,imported,pk,geom,srid,final):
        '''run table version apply diffs'''
        q = []
        if final:
            q.append("select table_version.ver_apply_table_differences('{original}','{imported}','{pk}')".format(original=original,imported=imported,pk=pk))
        else:
            si = imported.split('.')
            if not self._pktest(si[0],si[1]):
                q.append('alter table {imported} add primary key ({pk})'.format(imported=imported,pk=pk))
            if geom and srid:
                q.append("select UpdateGeometrySRID('{schema}','{imported}', '{geom}', {srid})".format(schema=si[0],imported=si[1],geom=geom,srid=srid))
                q.append("update {imported} set {geom} = ST_Transform({geom}::geometry,{srid}::integer)".format(imported=imported,geom=geom,srid=srid))
            #q.append("select table_version.ver_apply_table_differences('{original}','{imported}','{pk}')".format(original=original,imported=imported,pk=pk))
            #for i in q: print i
        return q
        
    def versiontables(self,tablelist,final=False):
        for section in tablelist:
            sec, tab = section
            for t in tab:
                t2 = self.cm.map[sec][t]['table']
                pk = self.cm.map[sec][t]['primary']
                geom = self.cm.map[sec][t]['geom'] if self.cm.map[sec][t].has_key('geom') else None
                srid = self.cm.map[sec][t]['srid'] if self.cm.map[sec][t].has_key('srid') else None
                snap = '{}.{}{}'.format(self.conf.database_schema,SNAP,t)
                original = '{}.x_{}'.format(self.conf.database_originschema,t2)
                imported = '{}.{}{}'.format(self.conf.database_schema,PREFIX,t)
                for q in self.qset(original,snap,imported,pk,geom,srid,final):
                    print ('pQ1',q)
                    Processor.attempt(self.conf,q)
                dst_t = '{}{}'.format(SNAP,t) if TEST else 'x_{}'.format(t2)
                self.gridtables(sec,t,dst_t)
                    
    def gridtables(self,sec,tab,tname):
        '''Look for grid specification and grid the table if found'''
        if self.cm.map.has_key(sec) and self.cm.map[sec].has_key(tab) and self.cm.map[sec][tab].has_key('grid'):
            e = External(self.conf)
            e.build(tname,self.cm.map[sec][tab]['grid'])
        

class External(object):
    externals = (('table_grid.sql',"select public.create_table_polygon_grid('{schema}', '{table}', '{column}', {xres}, {yres})"),)
    
    def __init__(self,conf):
        self.conf = conf
        
        
    def build(self,gridtable,colres):
        '''Create temp schema'''
        #self.db.connect()
        for file,query in self.externals:
            schema,func = re.search('select ([a-zA-Z_\.]+)',query).group(1).split('.')
            if not self._fnctest(schema, func):
                with open(file,'r') as handle:
                    text = handle.read()
                #self.db.pg_ds.ExecuteSQL(text)
                Processor.attempt(self.conf,text)
            col = colres['geocol']
            res = colres['res']
            dstschema = self.conf.database_schema if TEST else self.conf.database_originschema
            q = query.format(schema=dstschema, table=gridtable, column=col, xres=res, yres=res)
            print ('eQ1',q)
            Processor.attempt(self.conf,q)
        #self.db.disconnect()
            
    def _fnctest(self,s,t):
        '''Check whether the table had a primary key already. ExecuteSQL returns layer if successful OR null on error/no-result'''
        q = "select * from information_schema.routines \
             where routine_schema like '{s}' \
             and routine_name like '{t}'".format(s=s,t=t)
        print ('fQ2',q)
        return Processor.attempt(self.conf, q)
            
class PExpectException(Exception):pass
class PExpectSFTP(object):  
      
    def __init__(self,conf):
        self.conf = conf
        self.target = '{}@{}:{}'.format(self.conf.connection_ftpuser,self.conf.connection_ftphost,self.conf.connection_ftppath)
        self.opts = ['-o','PasswordAuthentication=yes',self.target]
        
    def fetch(self,dfile):
        pattern = getattr(self.conf,'{}_filepattern'.format(dfile))
        localpath,localfile = None,None
        filelist = []
        prompt = 'sftp> '
        get_timeout = 60.0
        sftp = pexpect.spawn('sftp',self.opts)
        try:
            if sftp.expect('(?i)password:')==0:
                sftp.sendline(self.conf.connection_ftppass)
                if sftp.expect(prompt) == 0:
                    sftp.sendline('ls')
                    if sftp.expect(prompt) == 0:
                        for fname in sftp.before.split()[1:]:
                            fmatch = re.match(pattern,fname)
                            if fmatch: filelist += [fname,]
                        fname = Processor.recent(filelist,pattern)
                        localfile = re.match(pattern,fname).group(0)
                        #break
                        if not localfile: 
                            raise PExpectException('Cannot find matching file pattern')
                    else:
                        raise PExpectException('Unable to access or empty directory at {}'.format(self.conf.connection_ftppath))
                    localpath = '{}/{}'.format(getattr(self.conf,'{}_localpath'.format(dfile)),localfile)
                    sftp.sendline('get {}'.format(localfile))
                    if sftp.expect(prompt,get_timeout) != 0:
                        raise PExpectException('Cannot retrieve file, {}/{}'.format(self.conf.connection_ftppath,localfile))
                    os.rename('./{}'.format(localfile),localpath)
                else: 
                    raise PExpectException('Password authentication failed')  
            else:
                raise PExpectException('Cannot initiate session using {}'.format(selt.opts))  
                
        except pexpect.EOF:
            raise PExpectException('End-Of-File received attempting connect')  
        except pexpect.TIMEOUT:
            raise PExpectException('Connection timeout occurred')  
        finally:
            sftp.sendline('bye')
            sftp.close()
            
        return localpath

class SimpleUI(object):
    '''Simple UI component added to provide debian installer target'''
    H = 100
    W = 100
    R = RAISED
    
    def __init__(self):
        self.master = TK.Tk()
        self.master.wm_title('DAB')
        self.mainframe = TK.Frame(self.master,height=self.H,width=self.W,bd=1,relief=self.R)
        self.mainframe.grid()
        self.initWidgets()
        self._offset(self.master)
        self.mainframe.mainloop()

    def initWidgets(self):
        title_row = 0
        select_row = 1
        button_row = 3
        
        #B U T T O N
        self.mainframe.selectbt = TK.Button(self.mainframe,  text='Start', command=self.start)
        self.mainframe.selectbt.grid( row=button_row,column=0,sticky=E)
 
        self.mainframe.quitbt = TK.Button(self.mainframe,    text='Quit',  command=self.quit)
        self.mainframe.quitbt.grid(row=button_row,column=1,sticky=E)
  
        #C H E C K B O X
        runlevel = TK.StringVar()
        runlevel.set('reject')
        for text,selection,col in OPTS:
            self.mainframe.rlev = TK.Radiobutton(self.mainframe, text=text, variable=runlevel, value=selection)#,indicatoron=False)
            self.mainframe.rlev.grid(row=int(select_row+abs(col/2)),column=int(col%2),sticky=W)
        self.mainframe.rlev_var = runlevel   
        
        #L A B E L
        self.mainframe.title = TK.Label(self.mainframe,text='Select DAB Operation')
        self.mainframe.title.grid(row=title_row,column=0,sticky=W)
   
        
    def quit(self):
        self.mainframe.quit()
        
    def start(self):
        self.ret_val = self.mainframe.rlev_var.get()
        self.master.withdraw()
        self.quit()
        
    def _offset(self,window):
        window.update_idletasks()
        w = window.winfo_screenwidth()
        h = window.winfo_screenheight()
        size = tuple(int(_) for _ in window.geometry().split('+')[0].split('x'))
        x = w/4 - size[0]/2
        y = h/4 - size[1]/2
        window.geometry("%dx%d+%d+%d" % (size + (x, y)))

   
def oneOrNone(a,options,args):
    '''is A in args OR are none of the options in args'''
    return a in args or not any([True for i in options if i in args]) 
     
def part1(args,ogrdb,v,c,m):            
    '''fetch data from sources and prepare import schema'''
    print ("Beginning meshblock/localities file download")
    t = ()
    SELECTION['ogr'] = ogrdb.d
    #SELECTION['ogr'] = DatabaseConn_ogr(c)
    s = PExpectSFTP(c)
    v.setup()
    topts = ('meshblock','nzlocalities')
    if oneOrNone('meshblock',topts,args): # len(args)==0 or 'meshblock' in args:
        mbk = Meshblock(c,SELECTION['ogr'],m,s)
        t += (mbk.run(),)
    if oneOrNone('nzlocalities',topts,args): # len(args)==0 or 'localities' in args:
        nzl = NZLocalities(c,SELECTION['ogr'],m,s) 
        t += (nzl.run(),)
    c.save('t',t)
    print ("Stopping post import for user validation, rerun with option 'transfer' to start again from this point")
    return t

def part2(v,t):            
    '''if data has been validated transfer to final schema'''
    print ("Begining table mapping and final data import")
    #if not t: t = _t
    v.versiontables(t,final=False)
    ###v.teardown()
    
def part3(v,t):            
    '''if data has been validated transfer to final schema'''
    print ("Begining table mapping and final data import")
    #if not t: t = _t
    v.versiontables(t,final=True)
    ###v.teardown()
    
def partX(v):
    '''User has signalled that this data load is corrupt, so delete it'''
    v.teardown()
    
'''
TODO
file name reader, db overwrite
'''
def main():  

    try:
        opts, args = getopt.getopt(sys.argv[1:], "vh", ["version","help"])
    except getopt.error as msg:
        print (msg)
        print ("for help use --help")
        sys.exit(2)
        
        
    for opt, val in opts:
        if opt in ("-h", "--help"):
            print (__doc__)
            sys.exit(0)
        elif opt in ("-v", "--version"):
            print (__version__)
            sys.exit(0)
                    
    if len(args)==0:
        sui = SimpleUI()
        #sui.mainframe.mainloop()
        args = [sui.ret_val,]
        
    process(args)
            
def process(args):
    t = () 
    _t = (('meshblock', ('statsnz_meshblock', 'statsnz_ta', 'meshblock_concordance')), ('nzlocalities', ('nz_locality',)))
    
    c = ConfReader()
    m = ColumnMapper(c)
    v = Version(c,m)
    
    global SELECTION
    with DB(c,'ogr') as ogrdb:           
        #if a 't' value is stored we dont want to pre-clean the import schema 
        ###t = v.setup()
        aopts = [a[1] for a in OPTS]
        if 'reject' in args: 
            partX(v)
            return
        #if prepare requested import files and recreate 't'
        t = part1(args,ogrdb,v,c,m) if oneOrNone('load', aopts,args) else c.read('t')
        #if transfer requested map and transfer using current 't'
        if oneOrNone('map',aopts,args): 
            part2(v,t)
        if oneOrNone('transfer',aopts,args): 
            part3(v,t)

if __name__ == "__main__":
    main()
    print ('finished')



