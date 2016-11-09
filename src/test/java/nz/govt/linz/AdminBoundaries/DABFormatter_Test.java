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
import org.junit.Test;

public class DABFormatter_Test {
	
	private static Map<String,Map<String,String>> formmap = new LinkedHashMap<>();
	private static List<List<String>> tablelist = new ArrayList<>();
	
	private DABFormatter formatter;
	private String form,table;
	private Document formdoc,tabledoc;
	
	private final static String FS_CM = "COLMAP_VALUE"; 
	private final static String FS_LG = "LEGEND_VALUE";
	private final static String FS_39 = "39_VALUE";
	
	private final static String TS_CP = "CAPTION_VALUE"; 
	private final static String TS_D2 = "ROW2_COL3"; 
	
	private final static int form_len = 1448;
	private final static int table_len = 266;
	
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {	
		//temp should be bypased, colmap should get textarea
		formmap.put("SECTION_1", new HashMap<String,String>(){{put("OPTION_1", "VALUE 1 A");put("OPTION_2", "VALUE 2 B");}});
		formmap.put("temp",      new HashMap<String,String>(){{put("OPTION_3", "VALUE 3 C");put("OPTION_4", "VALUE 4 D");}});
		formmap.put("SECTION_2", new HashMap<String,String>(){{put("OPTION_5", "VALUE 5 E");put("OPTION_6", "VALUE 6 F");}});
		formmap.put("SECTION_CM",new HashMap<String,String>(){{put("colmap", FS_CM);       put("OPTION_8", "VALUE 8 H");}});
		formmap.put("SECTION_3", new HashMap<String,String>(){{put("OPTION_9", FS_39);put("OPTION_0", "VALUE 0 J");}});
		
		tablelist.add(new ArrayList<String>(){{add("ROW1_COL1");add("ROW1_COL2");add("ROW1_COL3");}});
		tablelist.add(new ArrayList<String>(){{add("ROW2_COL1");add("ROW2_COL2");add(TS_D2);}});
		tablelist.add(new ArrayList<String>(){{add("ROW3_COL1");add("ROW3_COL2");add("ROW3_COL3");}});
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		formatter = new DABFormatter();
		form = formatter.formatForm(FS_LG,formmap);
		formdoc = Jsoup.parseBodyFragment(form);
		table = formatter.formatTable(TS_CP,tablelist);
		tabledoc = Jsoup.parseBodyFragment(table);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	
	/**
	 * Tests the string length of the generated test form
	 */
	@Test
	public void test_formChange() {
		assertEquals(form.length(),form_len);
	}
	
	/**
	 * Tests the string length of the generated test table
	 */
	@Test
	public void test_tableChange() {
		assertEquals(table.length(),table_len);
	}
	
	/**
	 * Tests whether selected values appear correctly in the form generator output
	 */
	@Test
	public void test_formValues(){
		assertEquals(FS_LG,formdoc.select("legend").first().text());
		assertEquals(FS_CM,formdoc.select("textarea").first().text());
		assertEquals(FS_39,formdoc.select("input[name=\"SECTION_3_OPTION_9\"]").first().val());
	}
	
	/**
	 * Tests whether selected values appear correctly in the table generator output
	 */
	@Test
	public void test_tableValues(){
		assertEquals(TS_CP,tabledoc.select("caption").first().text());
		assertEquals(TS_D2,tabledoc.select("tbody>tr>td").eq(2).text());
	}

}
