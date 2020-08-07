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

import nz.govt.linz.AdminBoundaries.DABContainerComp;
import nz.govt.linz.AdminBoundaries.DABIniReader;
import nz.govt.linz.AdminBoundaries.IniReader;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

//import org.jmock.Mockery;
//import org.jmock.Expectations;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DABContainerComp_Test {
	
	//private static Mockery context = new Mockery();
    
	private DABContainerComp container;	
	private static String[][] expected = {
		{"meshblock_concordance","meshblock_concordance","meshblock","MBC"},
		{"territorial_authority","statsnz_ta","ogc_fid","TA"},
		{"nz_locality","nz_locality","id","NZL"},
		{"meshblock","statsnz_meshblock","code","MB"}
	};

	private static String cm_mb  = "{ \"statsnz_meshblock\":{ \"table\":\"meshblock\", \"primary\":\"code\",},";
	private static String cm_mbc = "  \"meshblock_concordance\":{\"table\":\"meshblock_concordance\", \"primary\":\"meshblock\"},";
	private static String cm_ta  = "  \"statsnz_ta\":{\"table\":\"territorial_authority\", \"primary\":\"ogc_fid\"}}";
	
	private static String cm_nzl = "{ \"nz_locality\":{\"table\":\"nz_locality\", \"primary\":\"id\"}}";
	


	private static String CP = "testconfig.ini";
	private static String prefix = "temp_";
	private static IniReader ir;
	private static DABIniReader reader;
	
	@SuppressWarnings("serial")
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {		
		Map<String,Map<String,String>> restore = new HashMap<>();
		restore.put("user",new HashMap<String,String>(){{put("domain", "fake.domain.com");put("list", "user1,user2");}});
		restore.put("connection",new HashMap<String,String>(){{put("ftphost", "ftp.domain.com");put("ftpport", "999");}});
		restore.put("database",new HashMap<String,String>(){{put("host", "db.domain.com");put("port", "8080");put("prefix", prefix);}});
		restore.put("meshblock",new HashMap<String,String>(){{put("colmap", cm_mb+cm_mbc+cm_ta);}});
		restore.put("nzlocalities",new HashMap<String,String>(){{put("colmap", cm_nzl);}});

		ir = new IniReader(CP);
		ir.dump(restore);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		ir.flush();
	}

	@Before
	public void setUp() throws Exception {
		reader = new DABIniReader(CP);
		container = new DABContainerComp(reader);		
	}

	@After
	public void tearDown() throws Exception {
		reader = null;
		container = null;
	}	
	
	/**
	 * Test the ti_map object
	 */
	@Test
	public void test_10_ti(){
		for (int i=0; i<4; i++){
			assertEquals(expected[i][0],container.valueOf(expected[i][0]).dst());
			assertEquals(prefix+expected[i][1],container.valueOf(expected[i][0]).tmp());
			assertEquals(expected[i][2],container.valueOf(expected[i][0]).key());
			assertEquals(expected[i][3],container.valueOf(expected[i][0]).abv());
		}
	}	
	
	/**
	 * Test the reverse lookup of abbreviation vals to TI name
	 */
	@Test
	public void test_20_abv(){
		for (int i=0; i<4; i++){
			assertEquals(container.keyOf(expected[i][3]).dst(),expected[i][0]);
		}
	}
	

}
