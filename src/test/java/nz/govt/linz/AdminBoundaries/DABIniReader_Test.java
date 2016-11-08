package nz.govt.linz.AdminBoundaries;

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

import nz.govt.linz.AdminBoundaries.DABIniReader;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DABIniReader_Test {
	
	private String p;
	/** pre change reader */
	private DABIniReader reader1;
	/** editing reader */
	private DABIniReader reader2;
	/** post change reader */
	private DABIniReader reader3;
	
	private boolean change_flag;
	private Map<String,Map<String,String>> restore;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		change_flag = false;
		p = "testconfig.ini";
		restore = new HashMap<>();
		restore.put("user",new HashMap<String,String>(){{put("domain", "fake.domain.com");put("list", "user1,user2");}});
		restore.put("connection",new HashMap<String,String>(){{put("ftphost", "ftp.domain.com");put("ftpport", "999");}});
		restore.put("database",new HashMap<String,String>(){{put("host", "db.domain.com");put("port", "8080");}});
		changeReaders(p);
	}

	@After
	public void tearDown() throws Exception {
		restoreReaders(p);
	}
	
	private void changeReaders(String p) throws IOException {
		reader1 = new DABIniReader(p);
		reader2 = new DABIniReader(p);
		System.out.println("setup change");
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
		change_flag = true;
		reader3 = new DABIniReader(p);
	}
	
	private void restoreReaders(String p) throws IOException {
		System.out.println("setup restore");
		if (change_flag) {
			changeReaders(p);
			change_flag = false;
		}
	}
	

	@Test
	public void test_changed() {
		System.out.println("R1 "+reader1.getEntry("layer", "output_srid"));
		System.out.println("R2 "+reader2.getEntry("layer", "output_srid"));
		assertNotEquals(reader1.getEntry("database", "port"), reader3.getEntry("database", "port"));

	}	
	
	@Test
	public void test_unchanged() {
		assertEquals(reader1.getEntry("layer", "geom_column"), reader3.getEntry("layer", "geom_column"));

	}

}
