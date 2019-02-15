package nz.govt.linz.AdminBoundariesTest;

import nz.govt.linz.AdminBoundaries.UserAdmin.User;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReader;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReaderAIMS;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserAIMS;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UserReaderAIMS_Test {
	
	/*
	 * DELETE FROM aims_systemconfig.user_security WHERE username LIKE 'dummy%';
	 * ALTER SEQUENCE aims_systemconfig.user_security_seq RESTART WITH 22;
	 */
	
	/** reader obj */
	private UserReader reader;
	
	private static int user_count;// = 10;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {	
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		String[] p3 = UserReaderUtils.genParams(UserReaderAIMS.user_ref_base);
		reader = new UserReaderAIMS(p3[0],p3[1],p3[2]);
		user_count = reader.getUserList().size();
	}

	@After
	public void tearDown() throws Exception {
		//reader.save();
	}
	
	

	/**
	 * simple test of user count and known user types
	 */
	@Test
	public void test_10_checkUserList() {//*s for now, breaks test
		assertEquals(13,((UserAIMS)reader.findInUserList("****")).getUserId());
		assertEquals(17,((UserAIMS)reader.findInUserList("****")).getUserId());
	}
	
	/**
	 * test adding  user with minimal/default attributes
	 */
	@Test
	public void test_20_addUser1() {
		String dummyuser = "dummyuser1";
		//System.out.println("DR20.1 - before add "+reader.getUserList().size());
		reader.addUser(new UserAIMS(dummyuser));
		//System.out.println("DR20.2 - after add "+reader.getUserList().size());
		assertEquals(user_count+1,reader.getUserList().size());
	}
	
	/**
	 * test adding user with user defined attributes
	 */
	@Test
	public void test_21_addUser2() {
		String dummyuser = "dummyuser2";
		//System.out.println("DR21.1 - before add "+reader.getUserList().size());
		UserAIMS du2 = new UserAIMS(dummyuser);
		du2.setOrganisation("NZFS");
		du2.setRole("Reviewer");
		du2.setEmail("reginald.perrin@sunshinedesserts.co.uk");
		reader.addUser(du2);
		//System.out.println("DR21.2 - after add "+reader.getUserList().size());
		assertEquals(user_count+1,reader.getUserList().size());
	}
	
	/**
	 * read a user, edit the same user, merge and push back
	 * should see edits plus no change to non edits
	 */
	@Test
	public void test_30_editUser() {
		String dummyuser = "dummyuser2";
		//System.out.println("DR30.1 - before edit "+reader.getUserList().size());
		UserAIMS du2 = (UserAIMS) reader.findInUserList(dummyuser);
		String du2_u = du2.getUserName();
		String du2_e = du2.getEmail();
		String du2_o = du2.getOrganisation();
		String du2_r = du2.getRoleStr();
		du2.setEmail("martin.wellbourne@sunshinedesserts.co.uk");//was (but still is) Reggie
		du2.setOrganisation("Statistics NZ");//was nzfs
		du2.setRole("Publisher");//was reviewer
		reader.editUser(du2);
		//System.out.println("DR21.2 - after edit "+reader.getUserList().size());
		UserAIMS du2x = (UserAIMS) reader.findInUserList(dummyuser);
		assertEquals(du2_u,du2x.getUserName());
		assertNotEquals(du2_e,du2x.getEmail());
		assertNotEquals(du2_o,du2x.getOrganisation());
		assertNotEquals(du2_r,du2x.getRoleStr());
	}
	
	@Test
	public void test_40_deleteUser1() {
		//System.out.println("DR40.1 - before del "+reader.getUserList().size());
		reader.delUser("dummyuser1");
		//System.out.println("DR40.2 -after del "+reader.getUserList().size());
		assertEquals(user_count-1,reader.getUserList().size());
	}
	
	@Test
	public void test_41_deleteUser2() {
		//System.out.println("DR50.1 - before del "+reader.getUserList().size());
		reader.delUser("dummyuser2");
		//System.out.println("DR50.2 -after del "+reader.getUserList().size());
		assertEquals(user_count-1,reader.getUserList().size());
	}
	
	/**
	 * Convert user list to display table and count rows. 
	 * Should be user count plus header row
	 */
	@Test
	public void test_50_transformer() {
		List<List<String>> table_data = reader.transformUserList(reader.getUserList());
		//System.out.println("DR50-"+table_data);
		assertEquals(user_count+1,table_data.size());
	}

}
