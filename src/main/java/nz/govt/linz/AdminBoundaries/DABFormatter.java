package nz.govt.linz.AdminBoundaries;

import static nz.govt.linz.AdminBoundaries.DABServlet.ABs;
import static nz.govt.linz.AdminBoundaries.DABServlet.ABIs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

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

import nz.govt.linz.AdminBoundaries.DABContainerComp.ImportStatus;
import nz.govt.linz.AdminBoundaries.DABContainerComp.TableInfo;


public class DABFormatter {
	
	/**
	 * Creates out put formatted text from table data
	 */
	private static Map<String,String> lmtr;
	
	private static Map<Integer,List<String>> colourmap;
	
	protected static String BRED = "b_red";
	protected static String BGRN = "b_green";
	protected static String BYLW = "b_yellow";

	public DABFormatter(){
		lmtr = new LinkedHashMap<>(); //NB LMH preserves order
		lmtr.put("Load","Download new files from SFTP directory and build import tables");
		lmtr.put("Transfer","Run table_version function to populate destination tables");
		lmtr.put("Reject","Drop the import tables and quit");
		//lmtr.put("Notify","Notify users imports have completed and new data is ready to review");
		
		colourmap = new HashMap<>();
		colourmap.put(0, Arrays.asList(BGRN,BRED,BRED));
		colourmap.put(1, Arrays.asList(BYLW,BGRN,BGRN));
		colourmap.put(2, Arrays.asList(BYLW,BYLW,BGRN));
		
	}
	
	/**
	 * Reformats a list/list/string as an html table with the caption tname
	 * @param tname
	 * @param result
	 * @return
	 */	
	public static String getSummaryAsTable(String tname, List<List<String>> result) {
		String table = "";
	    table += "<table>";
	    table += "<caption>"+tname+"</caption>";
	    table += "<thead><tr>";
	    List<String> head = result.get(0);
	    for (String cell : head) {
	    	table += "<th>" +  cell + "</th>";
	    }
	    table += "</tr></thead><tbody>";
    	for (int i=1; i<result.size(); i++) {
    		table += "<tr>";
    		List<String> row = result.get(i);
    		for (String cell : row){
    			table += "<td>" + cell + "</td>";
    		}
    		table += "</tr>";
	    }

	    table += "</tbody></table>";
	    return table;
	}	
	
	public String getAcceptDeclineNav(int lowsts){   
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
	
	public String getAlternateNav(){   
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
