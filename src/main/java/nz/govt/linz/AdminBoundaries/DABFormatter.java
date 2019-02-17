package nz.govt.linz.AdminBoundaries;

/**
 * AdminBoundaries
 *
 * Copyright 2014 Crown copyright (c)
 * Land Information New Zealand and the New Zealand Government.
 * All rights reserved
 *
 * This program is released under the terms of the new BSD license. See the
 * LICENSE file for more information.
 */

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;


/**
 * Formatter class whose main function is to map resultset data into html tables/forms. Other
 * formatting functions also added include button arrays and info messages
 * @author jramsay
 *
 */
public class DABFormatter {
	
	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger(DABFormatter.class.getName());
	
	/** Main function button text array*/
	private static Map<String,String> lmtr;
	
	/** Colouration of function buttons array */
	private static Map<Integer,List<String>> colourmap;
	
	protected static String BRED = "b_red";
	protected static String BGRN = "b_green";
	protected static String BYLW = "b_yellow";
	protected static String BNAV = "b_nav";

	/**
	 * Formatter constructor sets up colourmap and button text
	 */
	public DABFormatter(){
		lmtr = new LinkedHashMap<>(); //NB LHM preserves order
		lmtr.put("Load","Download new files from SFTP directory and build import tables");
		lmtr.put("Transfer","Run table_version function to populate destination tables");
		lmtr.put("Reject","Drop all import tables");
		lmtr.put("Optional","Run optional post-processing functions");
		//lmtr.put("Notify","Notify users imports have completed and new data is ready to review");
		//TODO Link colourmap to lmtr
		colourmap = new HashMap<>();
		colourmap.put(0, Arrays.asList(BGRN,BRED,BRED,BGRN));
		colourmap.put(1, Arrays.asList(BYLW,BGRN,BGRN,BGRN));
		colourmap.put(2, Arrays.asList(BYLW,BYLW,BGRN,BGRN));
		
	}
	
	/**
	 * Simple UL list
	 * @param plist
	 * @return
	 */
	public static String formatList(Map<String, Map<String,String>> plist) {
		String res = "<ul>";
		for (String sec : plist.keySet()) {
			Map<String, String> kv = plist.get(sec);
			for (String k : kv.keySet()) {
				res += "<li>"+sec+" [ "+k+"="+kv.get(k)+" ]</li>";
			}
		}
		return res+"</ul>";
	}
	/**
	 * Reformats a list/list/string as an html table with the caption tname
	 * @param caption
	 * @param result
	 * @return
	 */	
	public static String formatTable(String caption, List<List<String>> result) {
		String table = "";
	    table += "<table>";
	    table += "<caption type=\"h2\">"+caption+"</caption>";
	    table += "<thead><tr>";
	    //LOGGER.info("Tsz:"+result.size());
	    List<String> head = result.get(0);
	    //LOGGER.info("H:"+String.join(",",head));
	    for (String cell : head) {
	    	table += "<th>" +  cell + "</th>";
	    }
	    table += "</tr></thead>\n<tbody>\n";
    	for (int i=1; i<result.size(); i++) {
    		table += "<tr>";
    		List<String> row = result.get(i);
    		//LOGGER.info("B:"+String.join(",",row));
    		for (String cell : row){
    			table += "<td>" + cell + "</td>";
    		}
    		table += "</tr>\n";
	    }

	    table += "</tbody>\n</table>\n";

	    return table;
	}

	/**
	 * Reformats a list/list/string as an html table with the caption tname
	 * @param fname
	 * @param config
	 * @return
	 */	
	public static String formatForm(String fname, Map<String, Map<String, String>> config) {
		String SEP = "_";
		String form = "";
	    form += "<article><form method=\"post\">\n";
	    form += "<legend>"+fname+"</legend>\n";

    	for (String section : config.keySet()) {
    		if ("temp".equals(section)) continue;//HACK
    		Map<String, String> opt_val = config.get(section);
    		form += "<label class=\"sec\">"+section+"</label>";
    		form += "<section class=\"form\">\n";
    		for (String option : opt_val.keySet()) {
    			//itype = "text";
    			//if ("colmap".equals(option)) itype = "textarea";
    			form += "<label for=\""+section+SEP+option+"\">"+section+"  "+option+"</label>\n";
    			
    			if ("colmap".equals(option) || "functions".equals(option)) {
    				form += "<textarea name=\""+section+SEP+option+"\">"+opt_val.get(option)+"</textarea><br/>\n";
    			}
    			else {
    				form += "<input name=\""+section+SEP+option+"\" value='"+opt_val.get(option)+"' class=\"sec\" type=\"text\"/><br/>\n";
    			}
    		}
    		form += "</section>\n";
	    }
    	form += "<section><input type=\"submit\" value=\"save\"/></section>";
	    form += "</form>\n</article>\n";
	    return form;
	}
	
	
	
	/**
	 * Returns main navigation/function buttons
	 * @param lowsts
	 * @return
	 */
	public String getNavigation(int lowsts){   
		int count = 0;
		String page = "sum";
		String msg = "<nav><ul>\n";
		String b_col,href;

		Iterator<Map.Entry<String,String>> lmtr_i = lmtr.entrySet().iterator();
		while (lmtr_i.hasNext()){
			Map.Entry<String,String> pair = (Map.Entry<String,String>) lmtr_i.next();
			b_col = colourmap.get(lowsts).get(count);
			if (b_col == BRED){
				href = "/ab";
			}
			else {
				href =  page + "?action=" + pair.getKey().toLowerCase();
			}

			msg += "<li><a href=\"" + href +  "\" class=\"" + b_col
				+  "\">" + pair.getKey() + "</a>" + pair.getValue() + "</li>\n";
			count ++;
		}
		msg += "</ul></nav>\n";
		return msg;
	}	
	
	/**
	 * Simple back button html
	 * @return
	 */
	public String getBackNav(){   
		String msg = "<nav><ul>\n";
		msg += "<li><a href=\"sum\" class=\"b_green\">BACK</a>Return to Main page</li>\n";
		msg += "</ul></nav>\n";
		return msg;
	}
	
	/**
	 * Formats a set of key/val pairs into an itemised list 
	 * @param kv
	 * @return
	 */
	protected String getInfoMessage(Map<String,String> kv){
		String li_str = "";
		if (kv.size()>0){
			for (String k : kv.keySet()){
				li_str += "<li><b>"+k+"</b> : "+kv.get(k)+"</li>\n";
			}
			return String.join("\n","<article>\n<ul>",li_str,"</ul>\n</article>\n");
		}
		else {
			return "";
		}
	}
	
	
	public String toString(){
		return "DABFormatter";
	}
	
}
