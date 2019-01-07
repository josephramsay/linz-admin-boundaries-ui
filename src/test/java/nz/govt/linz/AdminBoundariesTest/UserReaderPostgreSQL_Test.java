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
import nz.govt.linz.AdminBoundaries.UserReaderTomcat;
import nz.govt.linz.AdminBoundaries.UserReaderPostgreSQL;
import static org.junit.Assert.*;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.postgresql.ds.common.BaseDataSource;
import org.postgresql.jdbc2.optional.SimpleDataSource;

public class UserReaderPostgreSQL_Test {
	
	private static final String p = "testconfig.ini";
	//private static final String samplefile = "../../../../../../resourxes/tomcat-users.sample.xml";
	private static final String sampledb = "devassgeo01";
	
	/** reader obj */
	private UserReader reader;
	private SimpleDataSource datasource;

	
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
		datasource = new SimpleDataSource();
		datasource.setServerName("localhost");
		datasource.setDatabaseName("testdb" );
		datasource.setUser( "testuser0" );
		datasource.setPassword( "testpass0" );
		reader = new UserReaderPostgreSQL(datasource);
	}

	@After
	public void tearDown() throws Exception {
		reader.save();
	}
		
	@Test
	public void test_checkUserList() {
		List<Map<String,String>> user_list = reader.getUserList();
		assertEquals(6,user_list.size());
		assertEquals("testpass1",reader.findInUserList("testuser1").get("password"));
		assertEquals("testpass2",reader.findInUserList("testuser2").get("password"));
	}
	
/*	@Test
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
	}	*/


}
