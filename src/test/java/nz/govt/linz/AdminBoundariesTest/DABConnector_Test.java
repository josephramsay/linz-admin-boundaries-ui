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
import java.io.IOException;
import java.lang.NullPointerException;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.postgresql.ds.PGSimpleDataSource;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DABConnector_Test {
	
	public final static boolean USEMOCK = false;
	
	private static Mockery context;
	
	private static DataSource datasource_m;
	private static Connection connection_m;
	private static Statement statement_m;
	private static ResultSet resultset_m1, resultset_m2, resultset_m3, resultset_m4;
	private static ResultSetMetaData metadata_m1, metadata_m2, metadata_m3, metadata_m4;
	private static States state_m;	
	
	private final static String query_1 = "SELECT rolname,rolsuper,rolinherit FROM \"pg_roles\" LIMIT 1";
	private final static int result_cols1 = 3;
	private final static String[] colname_1 = {"rolname","rolsuper","rolinherit"};
	private final static String[] colvalue_1 = {"postgres","t","t"};
	
	private final static String query_2 = "SELECT true";
	private final static int result_cols2 = 1;
	private final static String result_200 = "bool";//m2bool
	private final static String result_210 = "t";
	
	private final static String query_3 = "SELECT 'A'";
	private final static int result_cols3 = 1;
	private final static String result_300 = "?column?";//m3str
	private final static String result_310 = "A";	
	
	private final static String query_4 = "SELECT COUNT(*) FROM \"pg_tablespace\"";
	private final static int result_cols4 = 1;
	private final static String result_400 = "count";//m3str
	private final static String result_410 = "2";
	
	private final static String query_e = "SELECT error";
	private final static String[] colvalue_e_p = {"SQLException","org.postgresql.util.PSQLException"};
	private final static String[] colvalue_e_m = {"SQLException","java.sql.SQLException"};
	private static String[] colvalue_e = {""};
    int colindex_l = 0;
    
	private DABConnector connector;	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		context = new Mockery();
		datasource_m = context.mock(DataSource.class);
		connection_m = context.mock(Connection.class);
		statement_m  = context.mock(Statement.class);
		resultset_m1 = context.mock(ResultSet.class, "table");
		resultset_m2 = context.mock(ResultSet.class, "single_rs_b");
		resultset_m3 = context.mock(ResultSet.class, "single_rs_s");
		resultset_m4 = context.mock(ResultSet.class, "count_rs_i");
		metadata_m1  = context.mock(ResultSetMetaData.class,"table_md");
		metadata_m2  = context.mock(ResultSetMetaData.class,"single_md_b");
		metadata_m3  = context.mock(ResultSetMetaData.class,"single_md_s");
		metadata_m4  = context.mock(ResultSetMetaData.class,"count_md_i");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		context = null;
		datasource_m = null;
		connection_m = null;
		statement_m  = null;
		resultset_m1 = null;
		resultset_m2 = null;
		resultset_m3 = null;
		resultset_m4 = null;
		metadata_m1  = null;
		metadata_m2  = null;
		metadata_m3  = null;
		metadata_m4  = null;
	}

	@Before
	public void setUp() throws Exception {
		if (USEMOCK) { 
			connector = getConnector_mock();
			colvalue_e = colvalue_e_m; }
		else { 
			connector = getConnector_pg(); 
			colvalue_e = colvalue_e_p;
		}
	}
	
	public DABConnector getConnector_pg() throws IOException {
		PGSimpleDataSource datasource_pg = new PGSimpleDataSource();
		Path path = FileSystems.getDefault().getPath("", "test_conf");
		List<String> dspg_args = Files.readAllLines(path,Charset.defaultCharset());
		datasource_pg.setServerName(dspg_args.get(0));
		datasource_pg.setDatabaseName(dspg_args.get(1));
		datasource_pg.setUser(dspg_args.get(2));
		datasource_pg.setPassword(dspg_args.get(3));
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
			allowing (statement_m).execute(query_4); then(state_m.is("cnt")); will(returnValue(true));//select c from t
			allowing (statement_m).execute(query_e); will(throwException(new SQLException()));
			allowing (statement_m).getResultSet(); will(returnValue(resultset_m2)); when(state_m.is("bool"));
			allowing (statement_m).getResultSet(); will(returnValue(resultset_m3)); when(state_m.is("str"));
			allowing (statement_m).getResultSet(); will(returnValue(resultset_m1)); when(state_m.is("lst"));
			allowing (statement_m).getResultSet(); will(returnValue(resultset_m4)); when(state_m.is("cnt"));
			
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
			
			//count result
			oneOf (resultset_m4).getMetaData(); will(returnValue(metadata_m4));
			oneOf (metadata_m4).getColumnCount(); will(returnValue(result_cols4));
			oneOf (metadata_m4).getColumnLabel(1); will(returnValue(result_400));
			atMost(1).of (resultset_m4).next(); will(returnValue(true));	
			atLeast(1).of (resultset_m4).next(); will(returnValue(false));
			allowing (resultset_m4).getString(result_400); will(returnValue(result_410));
			
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
	
	/**
	 * Tests the string length of the generated test table
	 */
	@Test
	public void test_20_execute_q2bool() {
		test_execute_qstr(query_2,result_200,result_210);
	}
	
	/**
	 * Tests the string length of the generated test form
	 */
	@Test
	public void test_30_execute_q3str() {
		test_execute_qstr(query_3,result_300,result_310);
	}
	/**
	 * Tests the string length of the generated test form
	 */
	@Test
	public void test_40_execute_q4count() {
		test_execute_qstr(query_4,result_400,result_410);
	}
	
	@Test
	public void test_41_execute_count() {
		if (!USEMOCK) {
			test_execute_qstr("select count(*) from admin_bdys_import.temp_meshblock_concordance","count","53596");
		}
	}
	
	private void test_execute_qstr(String query, String res0, String res1) {
		//System.out.println(connector.executeQuery(query_3));
		List<List<String>> result = connector.executeQuery(query);
		assertEquals(res0,result.get(0).get(0));
		assertEquals(res1,result.get(1).get(0));
	}
	
	
	/**
	 * Test the resultset parser
	 */
	@Test
	public void test_50_execute_table() {
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
	public void test_60_execute_sqlexception() {
		List<List<String>> result = connector.executeQuery(query_e);
		//System.out.println(result);
		int trunc = Math.min(result.get(0).get(1).length(),colvalue_e[1].length());
		assertEquals(result.get(0).get(0),colvalue_e[0]);
		assertEquals(result.get(0).get(1).substring(0,trunc),colvalue_e[1].substring(0,trunc));
	}
	
	/**
	 * Test the quotespace function
	 */
	@Test
	public void test_70_quotespace() {
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
	
	/**
	 * Tests the quotespace function throws an error on null input
	 */
	@Test(expected=NullPointerException.class)
	public void test_80_quotespace_err() {
		connector.quoteSpace(null);
	}

}
