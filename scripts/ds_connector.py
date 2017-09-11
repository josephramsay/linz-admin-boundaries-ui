#!/usr/bin/env python

'''
v.0.0.1

DSTransformer

Linrary module for DS conversion (taken from reblocker)
'''
 
import sys
import os
import re
import getopt
import psycopg2
import urllib
import json
import shutil
from abc import ABCMeta, abstractmethod
from collections import OrderedDict


PYVER3 = sys.version_info > (3,)

#2 to 3 imports
if PYVER3:
	import urllib.request as u_lib
	from urllib.error import HTTPError
else:
	import urllib2 as u_lib
	from urllib2 import HTTPError
	pass

try:
	import ogr, gdal
except ImportError:
	from osgeo import ogr, gdal

PG_CREDSFILE = '.pdb_credentials'
WFS_KEYFILE = '.stats_credentials'
KEY = None
KEYINDEX = 0

OVERWRITE = False
ENABLE_VERSIONING = False
DEF_SRS = 2193
USE_EPSG_WEBSERVICE = False
DST_SCHEMA = 'public'
DST_TABLE_PREFIX = 'new_'
DST_SUBDIR = '_new'
SHP_SUFFIXES = ('shp','shx','dbf','prj','cpg')
OGR_COPY_PREFS = ["OVERWRITE=NO","GEOM_TYPE=geometry","ENCODING=UTF-8"]

DEF_CREDS = '.pdb_credentials'
DEF_HOST = '127.0.0.1'
DEF_PORT = 5432

CONFIG = None
UICONFIG = None

if re.search('posix',os.name):
	DEF_SHAPE_PATH = ('/home/',)
else:
	DEF_SHAPE_PATH = ('C:\\',)
	
	
def setOverwrite(o):
	global OVERWRITE
	OVERWRITE = o
	global OGR_COPY_PREFS
	OGR_COPY_PREFS[0] = 'OVERWRITE={}'.format('YES' if o else 'NO')


class DatasourceException(Exception): pass
class DatasourceOpenException(DatasourceException): pass
class DatasourceReaderException(DatasourceException): pass
class _DS:
	
	__metaclass__ = ABCMeta
	
	FN_SPLIT = '###'
	CREATE = True
	uri = None
	dsl = {}
	driver = None
	
	def __init__(self):
		self.driver = ogr.GetDriverByName(self.DRIVER_NAME)
		
	def __enter__(self):
		return self
	
	def __exit__(self, type, value, traceback):
		for dsn in self.dsl:
			# print 'releasing',dsn
			if self.dsl[dsn]: self.dsl[dsn].SyncToDisk()
			self.dsl[dsn] = None
		self.dsl = None
		self.driver = None
		
	def _getprefs(self):
		return OGR_COPY_PREFS
		
	def _findSRID(self,name,sr,useweb):
		'''https://stackoverflow.com/a/10807867'''
		res = sr.AutoIdentifyEPSG()
		if res == 0:
			return sr.GetAuthorityCode(None)
		elif useweb:
			res = self._lookupSRID(sr.ExportToWkt())
			if res: return res
		print ('Warning. Layer {0} using DEF_SRS {1}'.format(name,DEF_SRS))	
		return DEF_SRS

	def _lookupSRID(self,wkt): 
		uu='http://prj2epsg.org/search.json?mode=wkt&terms='

		purl='127.0.0.1:3128'
		puser=None
		ppass=None
		pscheme="http"
		
		uuwkt = uu + u_lib.quote(wkt)
		
		handlers = [
				u_lib.HTTPHandler(),
				u_lib.HTTPSHandler(),
				u_lib.ProxyHandler({pscheme: purl})
			]
		opener = u_lib.build_opener(*handlers)
		u_lib.install_opener(opener)
		
		try:
			res = u_lib.urlopen(uuwkt)
			return int(json.loads(res.read())['codes'][0]['code'])
		except HTTPError as he:
			print ('SRS WS Convert Error {0}'.format(he))
		
	
	def initalise(self,dsn=None,create=True):
		ds = None
		try:
			upd = 1 if OVERWRITE else 0
			#ds = self.driver.Open(dsn, upd)
			#print ('DSN',dsn)
			ds = ogr.Open(dsn, upd)
			if ds is None:
				raise DatasourceException('Null DS {}'.format(dsn))
		except (RuntimeError, DatasourceException,Exception) as re1:
			if re.search('HTTP error code : 404',str(re1)):
				return None

			if self.CREATE: 
				try:
					ds = self.create(dsn)
				except RuntimeError as re2:
					raise DatasourceException('Cannot CREATE DS with {}. {}'.format(dsn,re2))
			else:
				raise DatasourceException('Cannot OPEN DS with {}. {}'.format(dsn,re1))
		finally:
			pass
			#ogr.UseExceptions()
		#print ('DS',ds)
		return ds
	
	def create(self,dsn):
		ds = None
		try:
			ds = self.driver.CreateDataSource(dsn, self._getprefs())
			if ds is None:
				raise DatasourceException("Error opening/creating DS "+str(dsn))
		except DatasourceException as ds1:
			raise
		except RuntimeError as re2:
			'''this is only caught if ogr.UseExceptions() is enabled (which we dont enable since RunErrs thrown even when DS completes)'''
			raise
		return ds
	
	@abstractmethod
	def read(self,filt): pass
	
	@abstractmethod
	def write(self): pass
	
	
class PGDS(_DS):
	INIT_VAL = 1
	DRIVER_NAME = 'PostgreSQL'
	#DBNAME = 'reblock'
	CS_TMPLT = 'PG:"dbname=#{1}# host=#{0}# port=#{2}# user=#{5}# password=#{3}# active_schema={4}"'

	cur = None
	conn = None
	cs = None
	
	def __init__(self):
		super(PGDS,self).__init__()
		#pstr = self.CS_TMPLT.format(*self.connstr().values())
		self.cs = self.connstr()
		self.dsl = {self.cs:self.initalise(self.cs, True)}
	
	def __enter__(self):
		self.connect()
		return super(PGDS,self).__enter__()
	
	def __exit__(self, type, value, traceback):
		self.disconnect()
		return super(PGDS,self).__exit__(type, value, traceback)
		
	def connstr(self):
		host,port = Authentication.hostport(PG_CREDSFILE)
		dbn = Authentication.selection(PG_CREDSFILE,'dbname')
		sch = Authentication.selection(PG_CREDSFILE,'schema')
		usr,pwd = Authentication.userpass(PG_CREDSFILE)
		#dd = {'DBHOST':host,'DBPORT':port,'SCHEMA':sch,'DBNAME':dbn,'USER':usr,'PASS':pwd}
		return self.CS_TMPLT.format(host,dbn,port,pwd,sch,usr).replace('#','\'')
		#return OrderedDict(sorted(dd.items(), key=lambda t: t[0]))
		
	def connect(self):
		if not self.cur:
			self.conn = psycopg2.connect(self.cs if self.cs  else self.connstr())
			self.cur = self.conn.cursor()
		
	def execute(self,qstr,results=False):
		success = self.cur.execute(qstr)
		return self.cur.fetchall() if results else not success or success
		
	def disconnect(self):
		self.cur.close()
		self.conn.commit()

	def read(self,filt):
		'''Read PG tables'''
		layerlist = {}
		#print ('DSL',self.dsl)
		for dsn in self.dsl:
			#print ('dsn',dsn)
			#print ('count',self.dsl[dsn].GetLayerCount())
			for index in range(self.dsl[dsn].GetLayerCount()):
				layer = self.dsl[dsn].GetLayerByIndex(index)
				name = layer.GetLayerDefn().GetName()
				#print ('PGLN',name)
				if name.find(DST_TABLE_PREFIX)==0 \
				and self._tname(name):
					#checks if (table)name is part of or in any of the filter items
					if not filt or max([1 if DST_TABLE_PREFIX + f == name else 0 for f in filt]) > 0: 
						srid = self._findSRID(name,layer.GetSpatialRef(),USE_EPSG_WEBSERVICE)
						layerlist[(dsn,name,srid)] = layer
		return layerlist
	
	def _tname(self,name):
		'''Debugging shortcut to add list of tables for export'''
		return True
		return name.find('new_contour')==0
	 
	def write(self,layerlist):
		'''Exports whatever is provided to it in layerlist'''
		return self.write_fast(layerlist)
	
	def write_fast(self,layerlist):
		'''PG write writes to a single DS since a DS represents a DB connection. SRID not transferred!'''
		self.connect()
		for dsn in layerlist:
			#print 'PG create layer {}'.format(dsn[1])
			list(self.dsl.values())[0].CopyLayer(layerlist[dsn],dsn[1],self._getprefs())
			'''HACK to set SRS'''
			q = "select UpdateGeometrySRID('{}','wkb_geometry',{})".format(dsn[1].lower(),dsn[2])
			res = self.execute(q)
			#print q,res
		self.disconnect()
		return layerlist

	def write_slow(self,layerlist):
		'''HACK to retain SRS 
		https://gis.stackexchange.com/questions/126705/how-to-set-the-spatial-reference-to-a-ogr-layer-using-the-python-api'''
		for dsn in layerlist:
			#print 'PG create layer {}'.format(dsn[1])
			dstsrs = ogr.osr.SpatialReference()
			dstsrs.ImportFromEPSG(dsn[2])
			dstlayer = self.dsl.values()[0].CreateLayer(dsn[1],dstsrs,layerlist[dsn].GetLayerDefn().GetGeomType(),self._getprefs())
			
			# adding fields to new layer
			layerdef = ogr.Feature(layerlist[dsn].GetLayerDefn())
			for i in range(layerdef.GetFieldCount()):
				dstlayer.CreateField(layerdef.GetFieldDefnRef(i))
			
			# adding the features from input to dest
			for i in range(0, layerlist[dsn].GetFeatureCount()):
				feature = layerlist[dsn].GetFeature(i)
				try:
					dstlayer.CreateFeature(feature)
				except ValueError as ve:
					print ('Error Creating Feature on Layer {}. {}'.format(dsn[1],ve))
					
		return layerlist
		
class PGDS_Version(PGDS):
	'''table versioning for PG'''
	def __init__(self,fname=None):
		super(PGDS_Version,self).__init__()

	def execute(self,qstr):
		edittext = self._genVerText()
		self.cur.execute("SELECT table_version.ver_create_revision('{0}');".format(edittext))
		res = self.cur.execute(qstr)
		self.cur.execute("SELECT table_version.ver_complete_revision();")
		return res
	
	def _genVerText(self):
		return 'this is my edit'
	
	def _enableVer(self,schema,table):
		self.cur.execute("SELECT table_version.ver_enable_versioning('{0}', '{1}');".format(schema,table))
	
	def _disableVer(self,schema,table):
		self.cur.execute("SELECT table_version.ver_disable_versioning('{0}', '{1}');".format(schema,table))

class SFDS(_DS):
	
	INIT_VAL = 1
	DRIVER_NAME = 'ESRI Shapefile'
	
	def __init__(self,fname=None):
		super(SFDS,self).__init__()
		#print 'SFDS',fname
		self._initpath(fname)
		
	def _initpath(self,fname):
		self.shppath = DEF_SHAPE_PATH
		if fname: 
			if os.path.isdir(fname):
				#if provided fname is a directory make this the source path
				self.shppath = (fname,)
				#and init all the files in this directory
				self.dsl = self._getFileDS()
			else:
				#if fname is a file init this alone
				fname = os.path.normpath(os.path.join(os.path.dirname(__file__),fname))
				self.dsl = {fname:self.initalise(fname, True)}
		else:
			self.dsl = self._getFileDS()
			
	def _getprefs(self):
		return []
	
	def connstr(self):
		#TODO do something intelligent here like read from a def dir?
		return 'shapefile.shp'
	
	def readdir(self,spath):
		return [fp for fp in os.listdir(spath) if re.search('.shp$',fp)]
		
	def _getFileDS(self):
		'''loop directories + shapefiles + layers'''
		shplist = {}
		for spath in self.shppath:
			for sfile in self.readdir(spath):#['shingle_poly.shp','building_poly.shp','airport_poly.shp']:
				shpname = os.path.join(spath,sfile)
				#recursive so potentially problematic on nested directories
				shplist[shpname] = SFDS(shpname).dsl[shpname]
		return shplist
		
	def read(self,filt):
		layerlist = {}
		for dsn in self.dsl:
			for index in range(self.dsl[dsn].GetLayerCount()):
				layer = self.dsl[dsn].GetLayerByIndex(index)
				name = layer.GetLayerDefn().GetName()
				#print ('SFLN',name)
				#checks if name is part of in any if the filter items
				if not filt or max([1 if f in name else 0 for f in filt])>0: 
					srid = self._findSRID(name,layer.GetSpatialRef(),USE_EPSG_WEBSERVICE)
					layerlist[(dsn,name,srid)] = layer
		return layerlist
	
	def write(self,layerlist,cropcolumn=None):
		'''TODO. Write new shp per layer overwriting existing'''
		for dsn in layerlist:
			srcname = re.sub('^'+DST_TABLE_PREFIX,'',dsn[1])
			srcpath = os.path.abspath(self.shppath[0]+DST_SUBDIR)
			srcfile = os.path.abspath(os.path.join(srcpath,srcname+'.shp'))
			if not os.path.exists(srcpath): os.mkdir(srcpath)
			if os.path.exists(srcfile): self.driver.DeleteDataSource(srcfile)
			dstds = self.driver.CreateDataSource(srcfile)
			cpy = dstds.CopyLayer(layerlist[dsn],srcname,self._getprefs())
			#this section hacked in to add delete column functionality
			if cropcolumn:
				col = cpy.GetLayerDefn().GetFieldIndex(cropcolumn)
				cpy.DeleteField(col)
			dstds.Destroy()
		return layerlist
	
	def _write(self,layerlist):
		'''Alternative shape writer'''
		for dsn in layerlist:
			#dsn = ("PG:dbname='reblock' host='127.0.0.1' port='5432' active_schema=public", 'new_native_poly')
			#srcname = dsn[1].split('.')[-1]
			srcname = re.sub('^'+DST_TABLE_PREFIX,'',dsn[1])
			#srcfile = os.path.abspath(os.path.join(self.shppath[0],'..'))#,srcname+'.shp'))
			srcfile = os.path.abspath(self.shppath[0]+DST_SUBDIR)#,srcname+'.shp'))
			if not os.path.exists(srcfile): os.mkdir(srcfile)
			#srcsrs = layerlist[dsn].GetSpatialRef()
			if OVERWRITE:
				try:
					self.driver.DeleteDataSource(srcfile)
				except:
					for suf in SHP_SUFFIXES:
						f = '{0}/{1}.{2}'.format(srcfile,srcname,suf)
						if os.path.isfile(f): 
							try: os.remove(f)
							except : shutil.rmtree(f, ignore_errors=True)
			dstds = self.driver.CreateDataSource(srcfile)
			dstds.CopyLayer(layerlist[dsn],srcname,self._getprefs())
			
		return layerlist

class WFSDS(_DS):
	gdal.SetConfigOption('OGR_WFS_PAGING_ALLOWED', 'YES')
	gdal.SetConfigOption('OGR_WFS_PAGE_SIZE', '10000')

	INIT_VAL = 1
	DRIVER_NAME = 'WFS'

	SRC_DSN = {'mb':'layer-40077','mbc':'table-40084','ta':'layer-39939'}
	DST_DSN = {'p1':'PG:host=prdassgeo01 db=linz_db'}
	
	#/wfs/layer-40077?service=WFS&request=GetCapabilities
	URI_C = 'https://{dom}/services;key={key}/wfs/{lid}?service=WFS&request=GetCapabilities'
	
	#/wfs?service=WFS&version=2.0.0&request=GetFeature&typeNames=layer-40077&count=3
	URI_F = 'https://{dom}/services;key={key}/wfs?service=WFS&version={ver}&request={req}&typeNames={lid}&count=3'
	
	CAPS  = 'https://{dom}/services;key={key}/wfs?service=WFS&request=GetCapabilities'
	
	def __init__(self,fname=None):
		super(WFSDS,self).__init__()
		self._ds = None	
		
	@property
	def ds(self): return self._ds
	@ds.setter
	def ds(self,value): self._ds = value
	
	def sourceURI(self,lid):
		'''URI method returns source filename/url'''
		#possible defaults?
		#fmt = 'GML2'
		dom = 'datafinder.stats.govt.nz'
		svc = 'WFS'
		ver = '2.0.0'
		req = 'GetFeature'
		#typ = "&typeName="+layername
		#fmt = "&outputFormat="+fmt
		return self.URI_C.format(dom=dom,key=KEY,ver=ver,req=req,lid=lid)
	
	def initDS(self,dsn=None):
		#dsn='https://data.linz.govt.nz/services;key=305723dd78bf4d89aaf968f07cc16c4f/wfs?service=WFS&version=2.0.0&request=GetFeature&typeNames=layer-50804&count=3'
		#dsn = self.CAPS
		'''initialise a DS for reading'''
		try:
			self.ds = self.driver.Open('WFS:'+dsn)
			if self.ds is None:
				raise DatasourceOpenException('Null DS returned attempting to open {}'.format(dsn))
		except RuntimeError as re1:
			ldslog.error(re1)
		return self.ds
	
	
	def getLayer(self):
		if self.ds.GetLayerCount():
			return self.ds.GetLayerByIndex(0)
		raise DatasourceReaderException("No layers detected in this datasource.")
	
	def createDS(self,dsn):
		dbopts = []
		try:
			ds = self.driver.CreateDataSource(dsn, dbopts)
			if ds is None:
				raise DatasourceReaderException("Error opening/creating DS "+str(dsn))
		except DSReaderException as ds1:
			#print "DSReaderException, Cannot create DS.",dsre2
			ldslog.error(ds1,exc_info=1)
			raise
		except RuntimeError as re2:
			'''this is only caught if ogr.UseExceptions() is enabled (which we dont enable since RunErrs thrown even when DS completes)'''
			#print "GDAL RuntimeError. Error creating DS.",rte
			ldslog.error(re2,exc_info=1)
			raise
		return ds if ds else None
	
	def read(self,dsl):
		ly = {}
		return ('dummy',)
		for i in dsl:
			lid = WFSDS.SRC_DSN[i]
			uri = self.sourceURI(lid)
			self.initDS(dsn=uri)
			ly[lid] = self.getLayer()
		return ly
		
	def write(self):
		pass


class Authentication(object):
	'''Static methods to read keys/user/pass from files'''
	
	@staticmethod
	def userpass(upfile):
		return (Authentication.searchfile(upfile,'username'),Authentication.searchfile(upfile,'password'))
	   
	@staticmethod
	def hostport(upfile):
		return (Authentication.searchfile(upfile,'dbhost'),Authentication.searchfile(upfile,'dbport'))	
	
	@staticmethod
	def selection(upfile,parameter):
		return Authentication.searchfile(upfile,parameter)
		
	@staticmethod
	def apikey(kfile,kk='key'):
		'''Returns current key from a keyfile advancing KEYINDEX on subsequent calls'''
		global KEYINDEX
		key = Authentication.searchfile(kfile,'{0}{1}'.format(kk,KEYINDEX))
		if not key:
			KEYINDEX = 0
			key = Authentication.searchfile(kfile,'{0}{1}'.format(kk,KEYINDEX))
		else:
			KEYINDEX += 1
		return key
	
	@staticmethod
	def creds(cfile):
		'''Read CIFS credentials file'''
		return (Authentication.searchfile(cfile,'username'),\
				Authentication.searchfile(cfile,'password'),\
				Authentication.searchfile(cfile,'domain','WGRP'))
	
	@staticmethod
	def searchfile(sfile,skey,default=None):
		#value = default
		#look in current then app then home
		spath = (os.path.dirname(__file__),os.path.join(os.path.dirname(__file__),'..'),os.path.expanduser('~'),'/')
		first = [os.path.join(p,sfile) for p in spath if os.path.exists(os.path.join(p,sfile))][0]
		with open(first,'r') as h:
			for line in h.readlines():
				k = re.search('^{key}=(.*)$'.format(key=skey),line)
				if k: return k.group(1)
		return default
	
	@staticmethod
	def getHeader(korb,kfile):
		'''Convenience method for auth header'''
		if korb.lower() == 'basic':
			b64s = base64.encodestring('{0}:{1}'.format(*Authentication.userpass(kfile))).replace('\n', '')
			return ('Authorization', 'Basic {0}'.format(b64s))
		elif korb.lower() == 'key':
			key = Authentication.apikey(kfile)
			return ('Authorization', 'key {0}'.format(key))
		return None # Throw something

	@staticmethod
	def _walk(sfile,skey,default,dir):
		'''Simple directory walker looking for named file in all sub dirs'''
		for p,d,f in os.walk(dir):
			if sfile in f: 
				return Authentication.searchfile(os.path.join(p,sfile), skey, default)
		raise FileNotFoundError('File not found during directory walk of {}'.format(dir))
	   
def test():
	global KEY, WFS_KEYFILE, PG_CREDSFILE
	
	try:
		opts, args = getopt.getopt(sys.argv[1:], "hw:p:", ["help","wfskey=","pg_creds="])
	except getopt.error as msg:
		print (msg)
		print ("for help use --help")
		sys.exit(2)
	
	#opts
	for o, a in opts:
		if o in ("-h", "--help"):
			print (__doc__)
			sys.exit(0)
		if o in ("-w", "--wfskey"):
			WFS_KEYFILE = a
		if o in ("-p", "--pgcreds"):
			PG_CREDSFILE = a

	KEY = Authentication.apikey(WFS_KEYFILE)
	
	wfsds = WFSDS()
	sfds = SFDS()
	pgds = PGDS()
	
	layer_selection = ['mb','mbc','ta']
	
	res = pgds.write(wfsds.read(layer_selection))
	
	
	
if __name__ == "__main__":

	test()
