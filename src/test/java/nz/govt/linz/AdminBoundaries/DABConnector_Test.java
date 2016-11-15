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

import nz.govt.linz.AdminBoundaries.DABConnector;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.jmock.Mockery;
import org.jmock.Expectations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class DABConnector_Test {
	
	private static Mockery context = new Mockery();
	
	private static DataSource datasource_m;
	private static Connection connection_m;
	private static Statement statement_m;
	private static ResultSet resultset_m1,resultset_m2;
	private static ResultSetMetaData metadata_m;
	
	private final static String query_b = "SELECT true";
	private final static boolean result_b = true;
	
	private final static String query_s = "SELECT 'A'";
	private final static String result_s = "r1c1";
	
	private final static int cols_l = 3;
	private final static String[] colname_l = {"one","two","three"};
	private final static String[] colvalue_l = {"r1c1","r1c2","r1c3"};//,{"r2c1","r2c2","r2c3"},{"r3c1","r3c2","r3c3"}};
	private final static String query_l = "SELECT cols FROM table";

	private final static String[] colvalue_e = {"SQLException","java.sql.SQLException"};
	private final static String query_e = "SELECT error";
	
    int colindex_l = 0;
    
	private DABConnector connector;	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {	
		datasource_m = context.mock(DataSource.class);
		connection_m = context.mock(Connection.class);
		statement_m = context.mock(Statement.class);
		resultset_m1 = context.mock(ResultSet.class, "table");
		resultset_m2 = context.mock(ResultSet.class, "single");
		metadata_m = context.mock(ResultSetMetaData.class);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		connector = new DABConnector(datasource_m);
		context.checking(new Expectations() {{
            allowing (datasource_m).getConnection(); will(returnValue(connection_m));
            allowing (connection_m).createStatement(); will(returnValue(statement_m));
            allowing (connection_m).close();
            allowing (statement_m).executeQuery(query_b); will(returnValue(resultset_m2));	
            allowing (statement_m).executeQuery(query_s); will(returnValue(resultset_m2));
            allowing (statement_m).executeQuery(query_l); will(returnValue(resultset_m1));
            allowing (statement_m).executeQuery(query_e); will(throwException(new SQLException()));
            //table one row RS
            oneOf (resultset_m1).getMetaData(); will(returnValue(metadata_m));
            atMost(1).of (resultset_m1).next(); will(returnValue(true));	
            atLeast(2).of (resultset_m1).next(); will(returnValue(false));	
            allowing (resultset_m1).getString(colname_l[0]); will(returnValue(colvalue_l[0]));
            allowing (resultset_m1).getString(colname_l[1]); will(returnValue(colvalue_l[1]));	
            allowing (resultset_m1).getString(colname_l[2]); will(returnValue(colvalue_l[2]));
            //first entry bool/str RS
            allowing (resultset_m2).next(); will(returnValue(true));	
            allowing (resultset_m2).getBoolean(1); will(returnValue(result_b));
            allowing (resultset_m2).getString(1); will(returnValue(result_s));
            
            allowing (metadata_m).getColumnCount(); will(returnValue(cols_l));
			allowing (metadata_m).getColumnLabel(1); will(returnValue(colname_l[0]));
			allowing (metadata_m).getColumnLabel(2); will(returnValue(colname_l[1]));
			allowing (metadata_m).getColumnLabel(3); will(returnValue(colname_l[2]));
        }});
	}

	@After
	public void tearDown() throws Exception {
	}	
	
	/**
	 * Tests the string length of the generated test form
	 */
	@Test
	public void test_executeSTR() {
		System.out.println(connector.executeSTRQuery(query_s));
		assertEquals(connector.executeSTRQuery(query_s),result_s);
	}
	
	/**
	 * Tests the string length of the generated test table
	 */
	@Test
	public void test_executeTF() {
		System.out.println(connector.executeTFQuery(query_b));
		assertEquals(connector.executeTFQuery(query_b),result_b);
	}
	
	/**
	 * Test the resultset parser
	 */
	@Test
	public void test_executeQuery1() {
		List<List<String>> result = connector.executeQuery(query_l);
		assertEquals(result.get(0).get(0),colname_l[0]);
		assertEquals(result.get(1).get(0),colvalue_l[0]);
		assertEquals(result.get(1).get(1),colvalue_l[1]);
		assertEquals(result.get(1).get(2),colvalue_l[2]);
	}	
	
	/**
	 * Test the resultset parser
	 */
	@Test
	public void test_executeQuery2() {
		List<List<String>> result = connector.executeQuery(query_e);
		assertEquals(result.get(0).get(0),colvalue_e[0]);
		assertEquals(result.get(0).get(1),colvalue_e[1]);

	}
	

}
