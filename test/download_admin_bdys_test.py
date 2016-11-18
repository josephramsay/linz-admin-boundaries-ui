
'''
v.0.0.1

download_admin_bdys_test.py

Copyright 2011 Crown copyright (c)
Land Information New Zealand and the New Zealand Government.
All rights reserved

This program is released under the terms of the new BSD license. See the 
LICENSE file for more information.

Tests classes in download_admin_bdys.py

Created on 09/11/2016

@author: jramsay
'''

import unittest
import inspect
import sys
import re
import sys
import os

sys.path.append('../scripts')

from download_admin_bdys import ColumnMapper
from download_admin_bdys import ConfReader
from download_admin_bdys import Version

#testlog = Logger.setup('test')

#user to init an address, simple text string read from a stored config file
user_text = 'aims_user'

class Test00_ConfReader(unittest.TestCase):
	
	def setUp(self):
		self.testdata = '["test1","test2"]'
		self.testname = 'test'
		self.cr = ConfReader()
		
	def tearDown(self):
		self.cr = None
	
	def test00_selfTest(self):
		'''Self tests making sure reader object is instantated correctly'''
		self.assertIsNotNone(self.cr, 'Failied to instantiale reader')
		self.assertIsNotNone(self.cr.parser, 'Failied to instantiale parser')
		
	def test10_save(self):
		'''Tests saving data to config without returning an error'''
		self.assertIsNone(self.cr.save(self.testname,self.testdata),'Save method returns non null value or error')
		
		
	def test20_read(self):
		'''Tests retrieval of data from config file'''
		self.assertEqual(self.testdata,self.cr.read(self.testname,flush=False),"Fetched data does not match saved data")
		self.assertEqual(self.testdata,self.cr.read(self.testname,flush=True),"Fetched data does not match saved data")
		self.assertEqual((),self.cr.read(self.testname,flush=True),"Fetched data should be empty after a flush")
		
class Test10_ColumnMapper(unittest.TestCase):
	
	def setUp(self):
		c = ConfReader()
		m = ColumnMapper(c)	
	
	def tearDown(self):
		pass

			
class Test20_Version(unittest.TestCase):
	
	def setUp(self):
		c = ConfReader()
		m = ColumnMapper(c)
		v = Version(c, m)
	
	def tearDown(self):
		pass
	
def suite():
	test_suite = unittest.TestSuite()
	test_suite.addTest(unittest.makeSuite(Test00_ConfReader,Test10_ColumnMapper,Test20_Version))
	return test_suite


if __name__ == "__main__":
	unittest.main()
