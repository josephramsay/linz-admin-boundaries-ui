package nz.govt.linz.AdminBoundariesTest;

import nz.govt.linz.AdminBoundaries.UserAdmin.UserReader;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReaderPostgreSQL;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserPostgreSQL;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserPostgreSQL.PGRoles;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.postgresql.ds.PGSimpleDataSource;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UserReaderPostgreSQL_Test {
	
	private static final String GROUP_DROP = 
		"DROP ROLE IF EXISTS aims_admin;" +
		"DROP ROLE IF EXISTS aims_reader;" +
		"DROP ROLE IF EXISTS aims_user;" +
		"DROP ROLE IF EXISTS non_aims_group;";
	
	private static final String GROUP_SETUP = 
		"CREATE ROLE aims_admin;" +
		"CREATE ROLE aims_reader;" +
		"CREATE ROLE aims_user;" +
		"CREATE ROLE non_aims_group;";
	
	private static final String USER_DROP = 
		"DROP ROLE IF EXISTS testuser1;" + 
		"DROP ROLE IF EXISTS testuser2;" + 
		"DROP ROLE IF EXISTS testuser3;" + 
		"DROP ROLE IF EXISTS testuser4;" +
		"DROP ROLE IF EXISTS testuser5;" + 
		"DROP ROLE IF EXISTS testuser6;";
	
	private static final String USER_SETUP = 
		"CREATE ROLE testuser1 LOGIN;" + 
		"GRANT aims_reader TO testuser1;" + 
		"CREATE ROLE testuser2 LOGIN;" + 
		"GRANT aims_admin TO testuser2;" + 
		"CREATE ROLE testuser3 LOGIN;" + 
		"GRANT aims_user TO testuser3;" + 
		"CREATE ROLE testuser4 LOGIN;" + 
		"GRANT non_aims_group TO testuser4;" +
		"CREATE ROLE testuser5 LOGIN;" + 
		"CREATE ROLE testuser6 LOGIN;";
	
	private static int user_count;
	
	private UserReader reader;

	private PGSimpleDataSource datasource;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try( Connection conn = initConnection().getConnection() ){
			Statement stmt = conn.createStatement();
			stmt.execute(USER_DROP);
			stmt.execute(GROUP_DROP);
			stmt.execute(GROUP_SETUP);
			stmt.execute(USER_SETUP);
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		try( Connection conn = initConnection().getConnection() ){
			Statement stmt = conn.createStatement();
			stmt.execute(USER_DROP);
			stmt.execute(GROUP_DROP);
		}
	}

	@Before
	public void setUp() throws Exception {
		datasource = initConnection();
		reader = new UserReaderPostgreSQL(datasource);
		//setupUsers();
		user_count = reader.getUserList().size();//t1+t2+t3=3
	}

	
	private static PGSimpleDataSource initConnection() {
		PGSimpleDataSource datasource = new PGSimpleDataSource();
		datasource.setServerName("localhost");
		datasource.setDatabaseName("testdb" );
		datasource.setUser( "testuser0" );
		datasource.setPassword( "testpass0" );
		return datasource;
	}
	
	
	@After
	public void tearDown() throws Exception {
		reader.save();
	}
		
	@Test
	public void test_10_checkUserList() {
		assertEquals(EnumSet.of(PGRoles.aims_reader),((UserPostgreSQL)reader.findInUserList("testuser1")).getRoles());
		assertEquals(EnumSet.of(PGRoles.aims_admin),((UserPostgreSQL)reader.findInUserList("testuser2")).getRoles());
		assertEquals(EnumSet.of(PGRoles.aims_user),((UserPostgreSQL)reader.findInUserList("testuser3")).getRoles());
	}
	
	/* GRANT aims_reader TO testuser5; */
	@Test
	public void test_20_addUser() {
		String dummyuser = "testuser5";//testuser5 must exist in test db but without aims_* roles
		String dummyrole = "aims_reader";
		((UserReaderPostgreSQL)reader).addUser(dummyuser,dummyrole);
		assertEquals(user_count+1,reader.getUserList().size());
		assertEquals(EnumSet.of(PGRoles.aims_reader),((UserPostgreSQL)reader.findInUserList(dummyuser)).getRoles());
	}	
	
	/* GRANT aims_admin TO testuser5; */
	@Test
	public void test_21_editUser() {
		String dummyuser = "testuser5";
		String dummyrole = "aims_admin";//adding an additional role shouldn't change the user count
		((UserReaderPostgreSQL)reader).editUser(dummyuser,dummyrole);
		assertEquals(user_count,reader.getUserList().size());
		assertEquals(EnumSet.of(PGRoles.aims_admin),((UserPostgreSQL)reader.findInUserList(dummyuser)).getRoles());
		//assertEquals(EnumSet.of(PGRoles.aims_reader,PGRoles.aims_admin),((UserPostgreSQL)reader.findInUserList(dummyuser)).getRoles());
	}
	
	@Test
	public void test_22_existsUser() {
		String dummyuser = "testuser5";
		assertTrue(reader.userExists(dummyuser));
		assertEquals(dummyuser,reader.findInUserList(dummyuser).getUserName());
	}
	
	/* GRANT aims_user TO testuser5; */
	@Test
	public void test_23_editUserMultiRoleStr() {
		String dummyuser = "testuser5";
		String dummyroles = "aims_admin,aims_reader,aims_user";
		((UserReaderPostgreSQL)reader).editUser(dummyuser,dummyroles);
		assertEquals(user_count,reader.getUserList().size());
		assertEquals(EnumSet.of(PGRoles.aims_admin,PGRoles.aims_reader,PGRoles.aims_user),((UserPostgreSQL)reader.findInUserList(dummyuser)).getRoles());
	}
	
	/* GRANT aims_admin TO testuser6;
	   GRANT aims_reader TO testuser6;
	   GRANT aims_user TO testuser6; */
	@Test
	public void test_24_addUserMultiRoleStr() {
		String dummyuser = "testuser6";
		String dummyroles = "aims_admin,aims_reader,aims_user";
		((UserReaderPostgreSQL)reader).addUser(dummyuser,dummyroles);
		assertEquals(user_count+1,reader.getUserList().size());
		assertEquals(EnumSet.of(PGRoles.aims_admin,PGRoles.aims_reader,PGRoles.aims_user),((UserPostgreSQL)reader.findInUserList(dummyuser)).getRoles());
	}
	
	/* REVOKE aims_admin FROM testuser5;
	   REVOKE aims_reader FROM testuser5;
	   REVOKE aims_user FROM testuser5;
	   REVOKE aims_admin FROM testuser6;
	   REVOKE aims_reader FROM testuser6;
	   REVOKE aims_user FROM testuser6; */
	@Test
	public void test_30_deleteUser() {
		String dummyuser1 = "testuser5";
		String dummyuser2 = "testuser6";
		reader.delUser(dummyuser1);
		reader.delUser(dummyuser2);
		assertEquals(user_count-2,reader.getUserList().size());
	}
	
	@Test
	public void test_31_notExistsUser() {
		String dummyuser1 = "testuser5";
		String dummyuser2 = "testuser6";
		assertFalse(reader.userExists(dummyuser1));
		assertFalse(reader.userExists(dummyuser2));
	}
	
	
	
	/**
	 * Get table transform and count rows = users + header row
	 */
	@Test
	public void test_90_transformer() {
		List<List<String>> table_data = reader.transformUserList(reader.getUserList());
		//System.out.println("DR40-"+table_data);
		assertEquals(user_count+1,table_data.size());
	}


}
