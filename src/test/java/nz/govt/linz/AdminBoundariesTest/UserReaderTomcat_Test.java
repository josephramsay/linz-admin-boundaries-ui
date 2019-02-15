package nz.govt.linz.AdminBoundariesTest;

import nz.govt.linz.AdminBoundaries.UserAdmin.User;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReader;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReaderTomcat;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserTomcat;

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
public class UserReaderTomcat_Test {
	
	private static int user_count;
	
	private static final String samplefile = "src/test/resources/tomcat-users.sample.xml";
	//private static final String samplefile = "/home/jramsay/git/linz-admin-boundaries-ui/src/main/resources/tomcat-users.sample.xml";
	
	/** reader obj */
	private UserReader reader;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {	
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		System.out.println("--- test ---");
		reader = new UserReaderTomcat(samplefile);
		user_count = reader.getUserList().size();
	}

	@After
	public void tearDown() throws Exception {
		reader.save();
	}
		
	@Test
	public void test_10_checkUserList() {
		assertEquals("user1pass",((UserTomcat)reader.findInUserList("user1")).getPassword());
		assertEquals("user2pass",((UserTomcat)reader.findInUserList("user2")).getPassword());
	}
	
	@Test
	public void test_20_addUser() {
		String dummyuser = "dummyuser";
		String dummypass = "dummypass";
		String dummyrole = "AIMS";
		//System.out.println("DR1-"+reader);
		UserTomcat user = new UserTomcat(dummyuser,dummypass,dummyrole);
		reader.addUser(user);
		//System.out.println("DR2-"+reader);
		assertEquals(user_count+1,reader.getUserList().size());
		assertEquals(dummypass,((UserTomcat)reader.findInUserList(dummyuser)).getPassword());
		//assertEquals(reader.encrypt(dummypass),((UserTomcat)reader.findInUserList(dummyuser)).getPassword());
	}
	
	@Test
	public void test_30_deleteUser() {
		reader.delUser("dummyuser");
		assertEquals(user_count-1,reader.getUserList().size());
	}
	
	@Test
	public void test_40_transformer() {
		List<List<String>> table_data = reader.transformUserList(reader.getUserList());
		//System.out.println("DR40-"+table_data);
		assertEquals(user_count+1,table_data.size());
	}


}
