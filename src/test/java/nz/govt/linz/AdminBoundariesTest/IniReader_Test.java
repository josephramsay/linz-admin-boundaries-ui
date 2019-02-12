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
	private IniReader reader1;
	
	private static Map<String,Map<String,String>> restore;
	
	/**
	 * Create a new hashmap and set the test values
	 * @throws Exception
	 */
	@SuppressWarnings("serial")
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {	
		//System.out.println("before class init");
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
		reader1 = new IniReader("r1"+test_path);
		reader1.dump(restore);
	}

	@After
	public void tearDown() throws Exception {
		System.out.println("teardown");
	}

	/**
	 * Test a small selection of values to make sure reader1 is being initialised
	 * @throws IOException
	 */
	@Test
	public void test_10_init() throws IOException {
		assertEquals(restore.get("user").get("domain"), reader1.getEntry("user", "domain"));
		assertEquals(restore.get("user").get("list"), reader1.getEntry("user", "list"));
		assertEquals(restore.get("connection").get("ftphost"), reader1.getEntry("connection", "ftphost"));
		assertEquals(restore.get("connection").get("ftpport"), reader1.getEntry("connection", "ftpport"));
		assertEquals(restore.get("database").get("host"), reader1.getEntry("database", "host"));
		assertEquals(restore.get("database").get("port"), reader1.getEntry("database", "port"));
	}
	
	/**
	 * Builds static instance reader from "restore" then dumps to a set path
	 * @throws IOException 
	 */
	@Test
	public void test_20_compare() throws IOException {
		IniReader reader2 = IniReader.getInstance(restore);
		reader2.setPath("r2"+test_path);
		reader2.dump();
		assertTrue(readerItemComparison(reader1,reader2));
	}
	
	/**
	 * Initialises a reader then sets from "restore" entry by entry
	 * @throws IOException 
	 */
	@Test
	public void test_30_compare() throws IOException {
		IniReader reader3 = new IniReader("r3"+test_path);
		for (String sec : restore.keySet()){
			for (String opt : restore.get(sec).keySet()){
				String newval = restore.get(sec).get(opt);
				reader3.setEntry(sec,opt,newval);
			}
		}
		reader3.dump();
		assertTrue(readerItemComparison(reader1,reader3));
	}
	
	/**
	 * Opens a new reader from the ini file saved by reader1. Tests read correctness
	 */
	@Test
	public void test_40_compare() {
		IniReader reader4 = new IniReader("r1"+test_path);
		reader4.load();
		assertTrue(readerItemComparison(reader1,reader4));
	}
	
	
	public boolean readerItemComparison(IniReader r1,IniReader r2) {
		for (String sec : r1.getSections()) {
			for (String opt : r1.getOptions(sec)) {
				if (!r1.getEntry(sec,opt).equals(r2.getEntry(sec,opt))) {
					return false;
				}
			}
		}
		return true;
	}	

}
