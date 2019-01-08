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
import org.junit.Test;

public class IniReader_Test {
	
	private static final String p = "testconfig.ini";
	/** pre change reader */
	private IniReader reader1;
	/** editing reader */
	private IniReader reader2;
	/** post change reader */
	private IniReader reader3;
	
	private static boolean overwrite_flag;
	private static Map<String,Map<String,String>> restore;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {	
		overwrite_flag = false;	
		restore = new HashMap<>();
		restore.put("user",new HashMap<String,String>(){{put("domain", "fake.domain.com");put("list", "user1,user2");}});
		restore.put("connection",new HashMap<String,String>(){{put("ftphost", "ftp.domain.com");put("ftpport", "999");}});
		restore.put("database",new HashMap<String,String>(){{put("host", "db.domain.com");put("port", "8080");}});
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		changeReaders(p);
	}

	@After
	public void tearDown() throws Exception {
		restoreReaders(p);
	}
	
	private void swapReaders(String p) throws IOException {
		reader1 = new IniReader(p);
		reader2 = new IniReader(p);
		for (String sec : reader2.getSections()){
			for (String opt : reader2.getOptions(sec)){
				if (restore.containsKey(sec) && restore.get(sec).containsKey(opt)){
					String oldval = reader2.getEntry(sec, opt);
					String newval = restore.get(sec).get(opt);
					System.out.println(oldval+"//"+newval);
					reader2.setEntry(sec,opt,newval);
					restore.get(sec).put(opt, oldval);
				}
			}
		}
		reader2.dump();
		reader3 = new IniReader(p);
	}
	
	private void restoreReaders(String p) throws IOException {
		System.out.println("setup restore");
		if (overwrite_flag) {
			swapReaders(p);
			overwrite_flag = false;
		}
	}
	
	private void changeReaders(String p) throws IOException {
		System.out.println("setup change");
		if (!overwrite_flag) {
			swapReaders(p);
			overwrite_flag = true;
		}
	}

	@Test
	public void test_changed() {
		assertNotEquals(reader1.getEntry("database", "port"), reader3.getEntry("database", "port"));
		assertNotEquals(reader1.getEntry("database", "host"), reader3.getEntry("database", "host"));
		assertNotEquals(reader1.getEntry("connection", "ftpport"), reader3.getEntry("connection", "ftpport"));
		assertNotEquals(reader1.getEntry("connection", "ftphost"), reader3.getEntry("connection", "ftphost"));
		assertNotEquals(reader1.getEntry("user", "domain"), reader3.getEntry("user", "domain"));
		assertNotEquals(reader1.getEntry("user", "list"), reader3.getEntry("user", "list"));

	}	
	
	@Test
	public void test_unchanged() {
		assertEquals(reader1.getEntry("layer", "geom_column"), reader3.getEntry("layer", "geom_column"));
		assertEquals(reader1.getEntry("layer", "grid_res"), reader3.getEntry("layer", "grid_res"));
		assertEquals(reader1.getEntry("user", "link"), reader3.getEntry("user", "link"));
		assertEquals(reader1.getEntry("user", "smtp"), reader3.getEntry("user", "smtp"));

	}
	
	@Test
	public void test_underscore(){
		assertEquals(reader1.getEntry("layer", "geom_column"), reader3.getEntry("layer", "geom_column"));
		assertEquals(reader1.getEntry("layer", "shift_geometry"), reader3.getEntry("layer", "shift_geometry"));
	}

}
