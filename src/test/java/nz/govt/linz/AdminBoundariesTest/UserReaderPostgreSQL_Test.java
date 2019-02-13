package nz.govt.linz.AdminBoundariesTest;

import nz.govt.linz.AdminBoundaries.UserAdmin.User;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReader;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReaderPostgreSQL;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserPostgreSQL;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserPostgreSQL.PGRoles;

import static org.junit.Assert.*;

import java.util.EnumSet;
import java.util.List;
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
	
	private static int user_count = 5;
	
	private UserReader reader;

	private PGSimpleDataSource datasource;
	
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
		assertEquals(user_count,user_list.size());
		assertEquals(EnumSet.of(PGRoles.aims_reader),((UserPostgreSQL)reader.findInUserList("testuser1")).getRoles());
		assertEquals(EnumSet.of(PGRoles.aims_admin),((UserPostgreSQL)reader.findInUserList("testuser2")).getRoles());
	}
	
	@Test
	public void test_20_addUser() {
		String dummyuser = "testuser5";//testuser5 must exist in test db but without aims_* roles
		String dummyrole = "aims_reader";
		((UserReaderPostgreSQL)reader).addUser(dummyuser,dummyrole);
		List<User> user_list = reader.getUserList();
		assertEquals(user_count+1,user_list.size());
		assertEquals(EnumSet.of(PGRoles.aims_reader),((UserPostgreSQL)reader.findInUserList(dummyuser)).getRoles());
	}	
	
	@Test
	public void test_21_addUser() {
		String dummyuser = "testuser5";
		String dummyrole = "aims_admin";//adding an additional role shouldn't change the user count
		((UserReaderPostgreSQL)reader).editUser(dummyuser,dummyrole);
		List<User> user_list = reader.getUserList();
		assertEquals(user_count+1,user_list.size());
		assertEquals(EnumSet.of(PGRoles.aims_admin),((UserPostgreSQL)reader.findInUserList(dummyuser)).getRoles());
		//assertEquals(EnumSet.of(PGRoles.aims_reader,PGRoles.aims_admin),((UserPostgreSQL)reader.findInUserList(dummyuser)).getRoles());
	}
	
	@Test
	public void test_22_existsUser() {
		String dummyuser = "testuser5";
		assertTrue(reader.userExists(dummyuser));
		assertEquals(dummyuser,reader.findInUserList(dummyuser).getUserName());
	}
	
	@Test
	public void test_23_editUserMultiRoleStr() {
		String dummyuser = "testuser5";
		String dummyroles = "aims_admin,aims_reader,aims_user";
		((UserReaderPostgreSQL)reader).editUser(dummyuser,dummyroles);
		List<User> user_list = reader.getUserList();
		assertEquals(user_count+1,user_list.size());
		assertEquals(EnumSet.of(PGRoles.aims_admin,PGRoles.aims_reader,PGRoles.aims_user),((UserPostgreSQL)reader.findInUserList(dummyuser)).getRoles());
	}
	
	@Test
	public void test_24_addUserMultiRoleStr() {
		String dummyuser = "testuser6";
		String dummyroles = "aims_admin,aims_reader,aims_user";
		((UserReaderPostgreSQL)reader).addUser(dummyuser,dummyroles);
		List<User> user_list = reader.getUserList();
		assertEquals(user_count+2,user_list.size());
		assertEquals(EnumSet.of(PGRoles.aims_admin,PGRoles.aims_reader,PGRoles.aims_user),((UserPostgreSQL)reader.findInUserList(dummyuser)).getRoles());
	}
	
	@Test
	public void test_30_deleteUser() {
		reader.delUser("testuser5");
		reader.delUser("testuser6");
		List<User> user_list = reader.getUserList();
		assertEquals(user_count,user_list.size());
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
		List<User> user_list = reader.getUserList();
		List<List<String>> table_data = reader.transformUserList(user_list);
		System.out.println("DR40-"+table_data);
		assertEquals(user_count+1,table_data.size());
	}


}
