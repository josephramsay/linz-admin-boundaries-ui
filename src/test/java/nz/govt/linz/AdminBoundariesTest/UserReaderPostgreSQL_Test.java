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
import nz.govt.linz.AdminBoundaries.UserReaderPostgreSQL;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
//import org.postgresql.jdbc2.optional.SimpleDataSource;//DEPRECATED
//import org.postgresql.jdbc3.Jdbc3SimpleDataSource;//DEPRECATED
import org.postgresql.ds.PGSimpleDataSource;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UserReaderPostgreSQL_Test {
	
	private static final String setupfile = "src/test/resourxes/test_setup.sql";
	
	private UserReader reader;
	//private Jdbc3SimpleDataSource datasource;
	//private SimpleDataSource datasource;
	private PGSimpleDataSource datasource;

	
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
		//System.out.println("--- test ---");
		datasource = new PGSimpleDataSource();
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
	public void test_10_checkUserList() {
		List<Map<String,String>> user_list = reader.getUserList();
		assertEquals(8,user_list.size());
		assertEquals("aims_dba",reader.findInUserList("testuser1").get("roles"));
		assertEquals("aims_admin",reader.findInUserList("testuser2").get("roles"));
	}
	
	@Test
	public void test_20_addUser() {
		String dummyuser = "dummyuser";
		String dummypass = "dummypass";
		String dummyrole = "aims_reader";
		reader.addUser(dummyuser,dummypass,dummyrole);
		List<Map<String,String>> user_list = reader.getUserList();
		assertEquals(9,user_list.size());
		assertEquals(reader.encrypt(dummypass),reader.findInUserList(dummyuser).get("password"));
	}
	
	@Test
	public void test_30_deleteUser() {
		reader.delUser("dummyuser");
		List<Map<String,String>> user_list = reader.getUserList();
		assertEquals(8,user_list.size());
	}


}
