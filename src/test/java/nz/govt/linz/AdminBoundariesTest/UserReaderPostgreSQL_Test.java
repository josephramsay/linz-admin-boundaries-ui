package nz.govt.linz.AdminBoundariesTest;

import nz.govt.linz.AdminBoundaries.UserAdmin.User;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReader;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReaderPostgreSQL;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserPostgreSQL;

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
		List<User> user_list = reader.getUserList();
		assertEquals(8,user_list.size());
		assertEquals("aims_dba",((UserPostgreSQL)reader.findInUserList("testuser1")).getRoles());
		assertEquals("aims_admin",((UserPostgreSQL)reader.findInUserList("testuser2")).getRoles());
	}
	
	@Test
	public void test_20_addUser() {
		String dummyuser = "dummyuser";
		String dummypass = "dummypass";
		String dummyrole = "aims_reader";
		((UserReaderPostgreSQL)reader).addUser(dummyuser,dummypass,dummyrole);
		List<User> user_list = reader.getUserList();
		assertEquals(9,user_list.size());
		assertEquals(reader.encrypt(dummypass),((UserPostgreSQL)reader.findInUserList(dummyuser)).getPassword());
	}
	
	@Test
	public void test_30_deleteUser() {
		reader.delUser("dummyuser");
		List<User> user_list = reader.getUserList();
		assertEquals(8,user_list.size());
	}


}
