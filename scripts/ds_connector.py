#!/usr/bin/env python

'''
v.0.0.1

DSTransformer

Library module for DS conversion (taken from reblocker)
'''
 
 # TODO
 # 1. set dest schema 					[x]
 # 2. remove gml_id from dest tables
 
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

from ds_auth import Authentication


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

OVERWRITE = False
ENABLE_VERSIONING = False
DEF_SRS = 2193
USE_EPSG_WEBSERVICE = False
DST_SCHEMA = 'public'
DST_TABLE_PREFIX = 'new_'
DST_SUBDIR = '_new'
SHP_SUFFIXES = ('shp','shx','dbf','prj','cpg')
OGR_COPY_PREFS = ["OVERWRITE=YES","GEOM_TYPE=geometry","ENCODING=UTF-8"]

DEF_CREDS = '.pdb_credentials'
DEF_HOST = '127.0.0.1'
DEF_PORT = 5432
DEF_SCHEMA='public'

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

class _DS(metaclass = ABCMeta):
	
	FN_SPLIT = '###'
	CREATE = True
	_uri = None
	_driver = None
	_ds = None
	
	@property
	def ds(self): return self._ds
	@ds.setter
	def ds(self,value): self._ds = value	
	
	@property
	def driver(self): return self._driver
	@driver.setter
	def driver(self,value): self._driver = value	
	
	@property
	def uri(self): return self._uri
	@uri.setter
	def uri(self,value): self._uri = value
	
	def __init__(self):
		self.driver = ogr.GetDriverByName(self.DRIVER_NAME)
		
	def __enter__(self):
		self.connect()
		return self
	
	def __exit__(self, type, value, traceback):
		self.disconnect()
		if self.ds: self.ds.SyncToDisk()
		self.ds = None
		self.driver = None
		
	@staticmethod
	def _getprefs(setschema=False):
		prefs = OGR_COPY_PREFS + (['SCHEMA={}'.format(DEF_SCHEMA)] if setschema else [])
		print ('DS prefs',prefs)
		return prefs
		
	@staticmethod
	def _findSRID(name,sr,useweb):
		'''https://stackoverflow.com/a/10807867'''
		res = sr.AutoIdentifyEPSG()
		if res == 0:
			return sr.GetAuthorityCode(None)
		elif useweb:
			res = _DS._lookupSRID(sr.ExportToWkt())
			if res: return res
		print ('Warning. Layer {0} using DEF_SRS {1}'.format(name,DEF_SRS))	
		return DEF_SRS

	@staticmethod
	def _lookupSRID(wkt): 
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
		
	@staticmethod
	def setDefSchema(schema):
		global DEF_SCHEMA
		DEF_SCHEMA = schema
	
	
	def initalise(self,dsn=None,create=True):
		'''Initialise a new DS using uri attribute'''
		try:
			upd = 1 if OVERWRITE else 0
			self.ds = self.driver.Open(dsn, upd)
			if self.ds is None:
				raise DatasourceException('Null DS {}'.format(dsn))
		except (RuntimeError, DatasourceException, Exception) as re1:
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

	
	def create(self,dsn):
		'''If writing a new layer first create it'''
		try:
			self.ds = self.driver.CreateDataSource(dsn, self._getprefs())
			if self.ds is None:
				raise DatasourceException("Error opening/creating DS "+str(dsn))
		except DatasourceException as ds1:
			raise
		except RuntimeError as re2:
			'''this is only caught if 
			ogr.UseExceptions() is enabled (which we dont enable since RunErrs thrown even when DS completes)'''

	
	@abstractmethod
	def read(self,id): 
		raise NotImplementedError('Read method not implemented')
	
	@abstractmethod
	def write(self,name,layer):
		raise NotImplementedError('Write method not implemented')
	
	@abstractmethod
	def connstr(self): 
		raise NotImplementedError
	
	@abstractmethod
	def connect(self):
		raise NotImplementedError
	
	@abstractmethod
	def disconnect(self):
		raise NotImplementedError
	
	
class PGDS(_DS):
	INIT_VAL = 1
	DRIVER_NAME = 'PostgreSQL'
	OVERWRITE = 'YES'

	CS_TMPLT = "PG: host={0} dbname='{1}' port='{2}' user='{5}' password='{3}' active_schema={4}"
	cur = None
	conn = None
	cs = None
	
	drop_cols_list = ['gml_id',]
	
	def __init__(self,ly_id=None):
		super(PGDS,self).__init__()
		#pstr = self.CS_TMPLT.format(*self.connstr().values())
		self.id = ly_id
		self.ds = None
		self.uri = PGDS.connstr()
		
	@staticmethod
	def connstr():
		host,port = Authentication.hostport(PG_CREDSFILE)
		dbn = Authentication.selection(PG_CREDSFILE,'dbname')
		sch = Authentication.selection(PG_CREDSFILE,'schema')
		PGDS.setDefSchema(sch)
		usr,pwd = Authentication.userpass(PG_CREDSFILE)
		return PGDS.CS_TMPLT.format(host,dbn,port,pwd,sch,usr)#.replace('#','\'')

		
	def connect(self):		
		self.initalise(self.uri, True)
		#if not self.cur:
		#	self.conn = psycopg2.connect(self.cs if self.cs  else self.connstr())
	
	def disconnect(self):
		pass
		#self.cur.close()
		#self.conn.commit()
		#	self.cur = self.conn.cursor()
		
	def execute(self,qstr,results=False):
		return self.ds.ExecuteSQL(qstr)
		#need to use psycopg2 and create a cursor for fetchall
		#return self.cur.fetchall() if results else success#not success or success
		


	def read(self,filt=None):
		'''Read PG tables'''
		layerlist = {}
		#print ('DSL',self.dsl)

		for index in range(self.ds.GetLayerCount()):
			layer = self.ds.GetLayerByIndex(index)
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
	 
	def write(self,ly):
		self.ds.CopyLayer(ly,self.id,self._getprefs())
		#self.postprocess()
		
	def postprocess(self):
		'''Do some final adjustments like removing auto added columns and aligning srids'''
		for c in self.drop_cols_list:
			print (self.execute("ALTER TABLE {t} DROP COLUMN {c}".format(t=self.id,c=c)))
		
		
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
	OVERWRITE = 'NO'
		
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
	
	#could use these for find/read file etc
	def connect(self): pass
	def disconnect(self): pass
		
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
	
	global KEY
	KEY = Authentication.apikey(WFS_KEYFILE)
	
	gdal.SetConfigOption('OGR_WFS_PAGING_ALLOWED', 'YES')
	gdal.SetConfigOption('OGR_WFS_PAGE_SIZE', '10000')

	INIT_VAL = 1
	DRIVER_NAME = 'WFS'
	OVERWRITE = 'NO'
	CREATE = False

	SRC_DSN = {
			'mb':('meshblock','layer-40077'),
			'mbc':('meshblock_concordance','table-40084'),
			'ta':('statsnz_ta','layer-39939')}
	DST_DSN = {'p1':'PG:host=prdassgeo01 db=linz_db'}
	
	#/wfs/layer-40077?service=WFS&request=GetCapabilities
	URI_C = 'WFS:https://{dom}/services;key={key}/wfs/{lid}'#?service=WFS&request=GetCapabilities'
	
	#/wfs?service=WFS&version=2.0.0&request=GetFeature&typeNames=layer-40077&count=3
	URI_F = 'https://{dom}/services;key={key}/wfs?service=WFS&version={ver}&request={req}&typeNames={lid}&count=3'
	
	CAPS  = 'https://{dom}/services;key={key}/wfs?service=WFS&request=GetCapabilities'
	
	def __init__(self,ly_id):
		super(WFSDS,self).__init__()
		self._ds = None	
		self.uri = WFSDS.connstr(ly_id)
		
	
	@staticmethod
	def connstr(lid):
		'''URI method returns source filename/url'''
		#fmt = 'GML2'
		dom = 'datafinder.stats.govt.nz'
		svc = 'WFS'
		ver = '2.0.0'
		req = 'GetFeature'
		#typ = "&typeName="+layername
		#fmt = "&outputFormat="+fmt
		return WFSDS.URI_C.format(dom=dom,key=KEY,ver=ver,req=req,lid=lid)
	
	def connect(self):
		#dsn='https://data.linz.govt.nz/services;key=305723dd78bf4d89aaf968f07cc16c4f/wfs?service=WFS&version=2.0.0&request=GetFeature&typeNames=layer-50804&count=3'
		#dsn = self.CAPS
		'''initialise a DS for reading'''	
		self.initalise(self.uri, True)
	
	def disconnect(self):
		self.ds = None
	
	
	def getLayer(self):
		if self.ds.GetLayerCount():
			return self.ds.GetLayerByIndex(0)
		raise DatasourceReaderException("No layers detected in this datasource.")
	
	def read(self):
		'''Read the layer from the configured DS identified by ly_id'''
		return self.getLayer()
		
	def write(self):
		pass



def test():
	global WFS_KEYFILE, PG_CREDSFILE
	
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
	
	layer_selection = ['ta','mb','mbc']
	for ly_ref in layer_selection:
		ly_id = WFSDS.SRC_DSN[ly_ref][1]
		ly_name = WFSDS.SRC_DSN[ly_ref][0]
		with WFSDS(ly_id) as wfsds:
			with PGDS(ly_name) as pgds:
				pgds.write(wfsds.read())
	
	
if __name__ == "__main__":

	test()
