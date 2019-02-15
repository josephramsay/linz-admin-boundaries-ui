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

import nz.govt.linz.AdminBoundaries.ProcessControl;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import java.io.File;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProcessControl_Test {

	private ProcessControl controller;	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {

	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}	
	
	/**
	 * Tests the string length of the generated test table
	 */
	@Test
	public void test_10_shstub() {	
		File testfile = new File("test/download_admin_bdys_stub.sh");
		controller  = new ProcessControl(testfile);	
		assertEquals("LOAD",controller.readProcessOutput(ProcessControl.getProcessBuilder("load"),""));
		assertEquals("MAP",controller.readProcessOutput(ProcessControl.getProcessBuilder("map"),""));
		assertEquals("TRANSFER",controller.readProcessOutput(ProcessControl.getProcessBuilder("transfer"),""));
	}	
	
	@Test
	public void test_20_pystub() {	
		File testfile = new File("test/download_admin_bdys_stub.py");
		controller  = new ProcessControl(testfile);
		assertEquals("LOAD",controller.readProcessOutput(ProcessControl.getProcessBuilder("load"),""));
		assertEquals("MAP",controller.readProcessOutput(ProcessControl.getProcessBuilder("map"),""));
		assertEquals("TRANSFER",controller.readProcessOutput(ProcessControl.getProcessBuilder("transfer"),""));
	}
	
	

}
