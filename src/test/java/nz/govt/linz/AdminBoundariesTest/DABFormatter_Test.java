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

import nz.govt.linz.AdminBoundaries.DABFormatter;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DABFormatter_Test {
	
	private static Map<String,Map<String,String>> formmap;
	private static List<List<String>> tablelist;
	
	private final static String FS_CM = "COLMAP_VALUE"; 
	private final static String FS_LG = "LEGEND_VALUE";
	private final static String FS_39 = "39_VALUE";
	
	private final static String TS_CP = "CAPTION_VALUE"; 
	private final static String TS_D2 = "ROW2_COL3"; 
	
	private final static int form_len = 1448;
	private final static int table_len = 271;
	
	
	@SuppressWarnings("serial")
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		//temp should be bypased, colmap should get textarea
		formmap = new LinkedHashMap<>();
		formmap.put("SECTION_1", new HashMap<String,String>(){{put("OPTION_1", "VALUE 1 A");put("OPTION_2", "VALUE 2 B");}});
		formmap.put("temp",      new HashMap<String,String>(){{put("OPTION_3", "VALUE 3 C");put("OPTION_4", "VALUE 4 D");}});
		formmap.put("SECTION_2", new HashMap<String,String>(){{put("OPTION_5", "VALUE 5 E");put("OPTION_6", "VALUE 6 F");}});
		formmap.put("SECTION_CM",new HashMap<String,String>(){{put("colmap", FS_CM);put("OPTION_8", "VALUE 8 H");}});
		formmap.put("SECTION_3", new HashMap<String,String>(){{put("OPTION_9", FS_39);put("OPTION_0", "VALUE 0 J");}});
		
		tablelist = new ArrayList<>();
		tablelist.add(new ArrayList<String>(){{add("ROW1_COL1");add("ROW1_COL2");add("ROW1_COL3");}});
		tablelist.add(new ArrayList<String>(){{add("ROW2_COL1");add("ROW2_COL2");add(TS_D2);}});
		tablelist.add(new ArrayList<String>(){{add("ROW3_COL1");add("ROW3_COL2");add("ROW3_COL3");}});
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		formmap = null;
		tablelist = null;
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	
	/**
	 * Tests the string length of the generated test form
	 */
	@Test
	public void test_10_formChange() {
		String form = DABFormatter.formatForm(FS_LG,formmap);
		assertEquals(form_len,form.length());
	}
	
	/**
	 * Tests the string length of the generated test table
	 */
	@Test
	public void test_20_tableChange() {
		String table = DABFormatter.formatTable(TS_CP,tablelist);
		System.out.println(tablelist);
		System.out.println(table);
		assertEquals(table_len,table.length());
	}
	
	/**
	 * Tests whether selected values appear correctly in the form generator output
	 */
	@Test
	public void test_30_formValues(){
		String form = DABFormatter.formatForm(FS_LG,formmap);
		Document formdoc = Jsoup.parseBodyFragment(form);
		assertEquals(FS_LG,formdoc.select("legend").first().text());
		assertEquals(FS_CM,formdoc.select("textarea").first().text());
		assertEquals(FS_39,formdoc.select("input[name=\"SECTION_3_OPTION_9\"]").first().val());
	}
	
	/**
	 * Tests whether selected values appear correctly in the table generator output
	 */
	@Test
	public void test_40_tableValues(){
		String table = DABFormatter.formatTable(TS_CP,tablelist);
		Document tabledoc = Jsoup.parseBodyFragment(table);
		assertEquals(TS_CP,tabledoc.select("caption").first().text());
		assertEquals(TS_D2,tabledoc.select("tbody>tr>td").eq(2).text());
	}

}
