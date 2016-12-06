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

import nz.govt.linz.AdminBoundaries.DABContainerComp;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

//import org.jmock.Mockery;
//import org.jmock.Expectations;

public class DABContainerComp_Test {
	
	//private static Mockery context = new Mockery();
    
	private DABContainerComp container;	
	private static String[][] d = {
		{"meshblock_concordance","meshblock_concordance","ogc_fid","MBC"},
		{"territorial_authority","statsnz_ta","ogc_fid","TA"},
		{"nz_locality","nz_locality","id","NZL"},
		{"meshblock","statsnz_meshblock","code","MB"}
	};
	
	private static String CP = "testconfig.ini";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {	
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		container = new DABContainerComp(CP);		
	}

	@After
	public void tearDown() throws Exception {
	}	
	
	/**
	 * Test the ti_map object
	 */
	@Test
	public void test_ti(){
		for (int i=0; i<4; i++){
			assertEquals(container.valueOf(d[i][0]).dst(),d[i][0]);
			assertEquals(container.valueOf(d[i][0]).tmp(),"temp_"+d[i][1]);
			assertEquals(container.valueOf(d[i][0]).key(),d[i][2]);
			assertEquals(container.valueOf(d[i][0]).abv(),d[i][3]);
		}
	}	
	
	/**
	 * Test the reverse lookup of abbreviation vals to TI name
	 */
	@Test
	public void test_abv(){
		for (int i=0; i<4; i++){
			assertEquals(container.keyOf(d[i][3]).dst(),d[i][0]);
		}
	}
	

}
