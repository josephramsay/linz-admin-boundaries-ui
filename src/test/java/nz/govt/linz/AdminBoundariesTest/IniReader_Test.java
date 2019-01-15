package nz.govt.linz.AdminBoundariesTest;

/**
 * AdminBoundaries Test
 *
 * Copyright 2014 Crown copyright (c)
 * Land Information New Zealand and the New Zealand Government.
 * All rights reserved
 *
 * This program is released under the terms of the new BSD license. See the
 * LICENSE file for more information.
 */

import nz.govt.linz.AdminBoundaries.IniReader;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class IniReader_Test {
	
	private static final String test_path = "testconfig.ini";
	/** reader instances each initialised seperately */
	private IniReader reader1, reader2, reader3, reader4;
	
	private static boolean overwrite_flag;
	private static Map<String,Map<String,String>> restore;
	
	/**
	 * Create a new hashmap and set the test values
	 * @throws Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {	
		//System.out.println("before class init");
		overwrite_flag = false;	
		restore = new HashMap<>();
		restore.put("user",new HashMap<String,String>(){{put("domain", "fake.domain.com");put("list", "user1,user2");}});
		restore.put("connection",new HashMap<String,String>(){{put("ftphost", "ftp.domain.com");put("ftpport", "999");}});
		restore.put("database",new HashMap<String,String>(){{put("host", "db.domain.com");put("port", "8080");}});
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		//System.out.println("after class");
		restore = null;
	}

	@Before
	public void setUp() throws Exception {
		System.out.println("setup");
		//changeReaders(test_path);
		try{init4Readers(test_path);}
		catch (IOException ioe) {fail("Init 4 Reader failed with "+ioe);}
	}

	@After
	public void tearDown() throws Exception {
		System.out.println("teardown");
		//restoreReaders(test_path);
		try{flush4Readers();}
		catch (Exception fnf_io_e) {fail("Flush 4 Reader failed with "+fnf_io_e);}
	}
	
	/**
	 * Load 3 reader instances; 
	 * r1. init with path and dumping entries to the path 
	 * r2. static instance with entries then dumped to a set path
	 * r3. init and set from restore entry by entry
	 * r4. opened from a saved r1
	 * @param test_initpath
	 * @throws IOException
	 */
	private void init4Readers(String test_initpath) throws IOException {
		//read r1 as blank file and set using dump(arg)
		reader1 = new IniReader("r1"+test_initpath);
		reader1.dump(restore);
		//init r2 using getinstance
		reader2 = IniReader.getInstance(restore);
		reader2.setPath("r2"+test_initpath);
		reader2.dump();
		//use setentry to set r3
		reader3 = new IniReader("r3"+test_initpath);
		for (String sec : restore.keySet()){
			for (String opt : restore.get(sec).keySet()){
				String newval = restore.get(sec).get(opt);
				reader3.setEntry(sec,opt,newval);
			}
		}
		reader3.dump();
		//read r4 from newly saved r1 file
		reader4 = new IniReader("r1"+test_initpath);
		reader4.load();
	}
	
	private void flush4Readers() throws FileNotFoundException, IOException {
		reader1.flush();
		reader2.flush();
		reader3.flush();
		reader4.flush();
	}
	
	/*
	private void restoreReaders(String test_restorepath) throws IOException {
		System.out.println("setup restore");
		if (overwrite_flag) {
			init4Readers(test_restorepath);
			overwrite_flag = false;
		}
	}
	
	private void changeReaders(String test_changepath) throws IOException {
		System.out.println("setup change");
		if (!overwrite_flag) {
			init4Readers(test_changepath);
			overwrite_flag = true;
		}
	}*/

	@Test
	public void test_10_changed() {
		for (String sec : reader1.getSections()) {
			for (String opt : reader1.getOptions(sec)) {
				assertEquals(reader1.getEntry(sec,opt), reader2.getEntry(sec,opt));
				assertEquals(reader1.getEntry(sec,opt), reader3.getEntry(sec,opt));
				assertEquals(reader1.getEntry(sec,opt), reader4.getEntry(sec,opt));
			}
		}
	}	

}
