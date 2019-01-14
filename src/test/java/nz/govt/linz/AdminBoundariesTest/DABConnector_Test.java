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

import nz.govt.linz.AdminBoundaries.DABConnector;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;
import org.jmock.Mockery;
import org.jmock.Expectations;
import org.jmock.States;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import java.lang.NullPointerException;

import org.postgresql.ds.PGSimpleDataSource;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DABConnector_Test {
	
	private static Mockery context = new Mockery();
	
	private static DataSource datasource_m;
	private static Connection connection_m;
	private static Statement statement_m;
	private static ResultSet resultset_m1, resultset_m2, resultset_m3;
	private static ResultSetMetaData metadata_m1, metadata_m2, metadata_m3;
	private static States state_m;	
	
	private final static String query_1 = "SELECT cols FROM table";
	private final static int result_cols1 = 3;
	private final static String[] colname_1 = {"one","two","three"};
	private final static String[] colvalue_1 = {"r1c1","r1c2","r1c3"};//,{"r2c1","r2c2","r2c3"},{"r3c1","r3c2","r3c3"}};
	
	private final static String query_2 = "SELECT true";
	private final static int result_cols2 = 1;
	private final static String result_200 = "m2bool";
	private final static String result_210 = "t";
	
	private final static String query_3 = "SELECT 'A'";
	private final static int result_cols3 = 1;
	private final static String result_300 = "m3str";
	private final static String result_310 = "A";

	private final static String[] colvalue_e = {"SQLException","java.sql.SQLException"};
	private final static String query_e = "SELECT error";
	
    int colindex_l = 0;
    
	private DABConnector connector;	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {	
		datasource_m = context.mock(DataSource.class);
		connection_m = context.mock(Connection.class);
		statement_m  = context.mock(Statement.class);
		resultset_m1 = context.mock(ResultSet.class, "table");
		resultset_m2 = context.mock(ResultSet.class, "single_rs_b");
		resultset_m3 = context.mock(ResultSet.class, "single_rs_s");
		metadata_m1   = context.mock(ResultSetMetaData.class,"table_md");
		metadata_m2   = context.mock(ResultSetMetaData.class,"single_md_b");
		metadata_m3   = context.mock(ResultSetMetaData.class,"single_md_s");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		connector = getConnector_mock();
	}
	
	public DABConnector getConnector_pg() {
		PGSimpleDataSource datasource_pg = new PGSimpleDataSource();
		datasource_pg.setServerName("localhost");
		datasource_pg.setDatabaseName("testdb" );
		datasource_pg.setUser( "testuser0" );
		datasource_pg.setPassword( "testpass0" );
		return new DABConnector(datasource_pg);
	}

	
	public DABConnector getConnector_mock() throws SQLException {
		state_m = context.states("query_state").startsAs("null");
		DABConnector connector_m = new DABConnector(datasource_m);
		context.checking(new Expectations() {{
			allowing (datasource_m).getConnection(); will(returnValue(connection_m));
			allowing (connection_m).createStatement(); will(returnValue(statement_m));
			allowing (connection_m).close();
			allowing (statement_m).execute(query_2); then(state_m.is("bool")); will(returnValue(true));//select true
			allowing (statement_m).execute(query_3); then(state_m.is("str")); will(returnValue(true));//select A
			allowing (statement_m).execute(query_1); then(state_m.is("lst")); will(returnValue(true));//select c from t
			allowing (statement_m).execute(query_e); will(throwException(new SQLException()));
			allowing (statement_m).getResultSet(); will(returnValue(resultset_m2)); when(state_m.is("bool"));
			allowing (statement_m).getResultSet(); will(returnValue(resultset_m3)); when(state_m.is("str"));
			allowing (statement_m).getResultSet(); will(returnValue(resultset_m1)); when(state_m.is("lst"));
			
			//single bool result
			oneOf (resultset_m2).getMetaData(); will(returnValue(metadata_m2));
			oneOf (metadata_m2).getColumnCount(); will(returnValue(result_cols2));
			oneOf (metadata_m2).getColumnLabel(1); will(returnValue(result_200));
			atMost(1).of (resultset_m2).next(); will(returnValue(true));
			atLeast(1).of (resultset_m2).next(); will(returnValue(false));
			never (resultset_m2).getBoolean(1); will(returnValue(result_210));
			allowing (resultset_m2).getString(result_200); will(returnValue(result_210));

			//single str result
			oneOf (resultset_m3).getMetaData(); will(returnValue(metadata_m3));
			oneOf (metadata_m3).getColumnCount(); will(returnValue(result_cols3));
			oneOf (metadata_m3).getColumnLabel(1); will(returnValue(result_300));
			atMost(1).of (resultset_m3).next(); will(returnValue(true));	
			atLeast(1).of (resultset_m3).next(); will(returnValue(false));
			allowing (resultset_m3).getString(result_300); will(returnValue(result_310));
			
			//table with 3 cols and 1 row
			oneOf (resultset_m1).getMetaData(); will(returnValue(metadata_m1));
			allowing (metadata_m1).getColumnCount(); will(returnValue(result_cols1));
			allowing (metadata_m1).getColumnLabel(1); will(returnValue(colname_1[0]));
			allowing (metadata_m1).getColumnLabel(2); will(returnValue(colname_1[1]));
			allowing (metadata_m1).getColumnLabel(3); will(returnValue(colname_1[2]));
			atMost(1).of (resultset_m1).next(); will(returnValue(true));	
			atLeast(1).of (resultset_m1).next(); will(returnValue(false));	
			allowing (resultset_m1).getString(colname_1[0]); will(returnValue(colvalue_1[0]));
			allowing (resultset_m1).getString(colname_1[1]); will(returnValue(colvalue_1[1]));	
			allowing (resultset_m1).getString(colname_1[2]); will(returnValue(colvalue_1[2]));
		}});
		return connector_m;
	}

	@After
	public void tearDown() throws Exception {
	}	
	
//	@Test
//	public void test_05_executeLocalhost() {
//		PGSimpleDataSource datasource = new PGSimpleDataSource();
//	
//		datasource.setServerName("localhost");
//		datasource.setDatabaseName("testdb" );
//		datasource.setUser( "testuser0" );
//		datasource.setPassword( "testpass0" );
//		connector = new DABConnector(datasource);
//		System.out.println(connector.executeQuery(query_2));
//		assertEquals(connector.executeQuery(query_2),result_2);
//	}
	
	
	/**
	 * Tests the string length of the generated test table
	 */
	@Test
	public void test_10_execute_q2bool() {
		//System.out.println(connector.executeQuery(query_2).get(1).get(0));
		List<List<String>> result = connector.executeQuery(query_2);
		assertEquals(result_200,result.get(0).get(0));//check its returning a bool
		assertEquals(result_210,result.get(1).get(0));//check that bool is true
	}
	
	/**
	 * Tests the string length of the generated test form
	 */
	@Test
	public void test_20_execute_q3str() {
		//System.out.println(connector.executeQuery(query_3));
		List<List<String>> result = connector.executeQuery(query_3);
		assertEquals(result_300,result.get(0).get(0));
		assertEquals(result_310,result.get(1).get(0));
	}
	
	
	/**
	 * Test the resultset parser
	 */
	@Test
	public void test_30_execute_mocktable() {
		List<List<String>> result = connector.executeQuery(query_1);
		//System.out.println(result);
		assertEquals(colname_1[0],result.get(0).get(0));
		assertEquals(colvalue_1[0],result.get(1).get(0));
		assertEquals(colvalue_1[1],result.get(1).get(1));
		assertEquals(colvalue_1[2],result.get(1).get(2));
	}	
	
	/**
	 * Test the resultset parser
	 */
	@Test
	public void test_40_execute_sqlexception() {
		List<List<String>> result = connector.executeQuery(query_e);
		System.out.println(result);
		assertEquals(result.get(0).get(0),colvalue_e[0]);
		assertEquals(result.get(0).get(1),colvalue_e[1]);

	}
	
	/**
	 * Test the quotespace function
	 */
	@Test
	public void test_50_quotespace() {
		assertEquals(connector.quoteSpace("col1,col2,col 3,col_4"),"col1,col2,'col 3',col_4");
		assertEquals(connector.quoteSpace("col1,col2,col 3 4,col 5"),"col1,col2,'col 3 4','col 5'");
		assertEquals(connector.quoteSpace("col1col2"),"col1col2");
		assertEquals(connector.quoteSpace(""),"");
		/*try{
			connector.quoteSpace(null);
			fail("Expected NPE");
		}
		catch (NullPointerException npe){
			assertEquals("",npe.getMessage());
		}*/
	}
	@Test(expected=NullPointerException.class)
	public void test_60_quotespace_err() {
		connector.quoteSpace(null);
	}
	
	

}
