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

//import UserReader;
import nz.govt.linz.AdminBoundaries.UserReader;
import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class UserReader_Test {
	
	private static final String p = "testconfig.ini";
	//private static final String samplefile = "../../../../../../resourxes/tomcat-users.sample.xml";
	private static final String samplefile = "/home/jramsay/git/linz-admin-boundaries-ui/src/main/resources/tomcat-users.sample.xml";
	
	/** reader obj */
	private UserReader reader;

	
	private static boolean overwrite_flag;
	private static Map<String,Map<String,String>> restore;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {	
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		System.out.println("--- test ---");
		File sample = new File(samplefile);
		reader = new UserReader(sample);
	}

	@After
	public void tearDown() throws Exception {
		reader.saveFile();
	}
		
	@Test
	public void test_checkUserList() {
		List<Map<String,String>> user_list = reader.getUserList();
		assertEquals(5,user_list.size());
		assertEquals("user1pass",reader.findInUserList("user1").get("password"));
		assertEquals("user2pass",reader.findInUserList("user2").get("password"));
	}
	
	@Test
	public void test_addUser() {
		String dummyuser = "dummyuser";
		String dummypass = "dummypass";
		String dummyrole = "dummyrole";
		reader.addUser(dummyuser,dummypass,dummyrole);
		List<Map<String,String>> user_list = reader.getUserList();
		assertEquals(6,user_list.size());
		assertEquals(UserReader.encrypt(dummypass),reader.findInUserList(dummyuser).get("password"));
	}
	
	@Test
	public void test_deleteUser() {
		reader.delUser("dummyuser");
		List<Map<String,String>> user_list = reader.getUserList();
		assertEquals(5,user_list.size());
	}	


}
