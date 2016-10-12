package nz.govt.linz.AdminBoundaries;

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


public class DABFormatter {
	
	/**
	 * Creates out put formatted text from table data
	 */
	private static Map<String,String> lmtr;

	public DABFormatter(){
		lmtr = new LinkedHashMap<>(); 
		lmtr.put("Load","Download new files from SFTP directory and build import tables");
		lmtr.put("Transfer","Run table_version function to populate destination tables");
		lmtr.put("Reject","Drop the import tables and quit");
		
	}
	
	/**
	 * Reformats a list/list/string as an html table with the caption tname
	 * @param tname
	 * @param result
	 * @return
	 */	
	public String getSummaryAsTable(String tname, List<List<String>> result) {
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
		String b_col = "b_yellow";

		Iterator<Map.Entry<String,String>> lmtr_i = lmtr.entrySet().iterator();
		while (lmtr_i.hasNext()){
			Map.Entry<String,String> pair = (Map.Entry<String,String>)lmtr_i.next();
			if (count >= lowsts) {b_col="b_green";}
			msg += "<li><a href=\""+page+"?action="+pair.getKey().toLowerCase()+"\" class=\""+b_col+"\">"+pair.getKey()+"</a>"+pair.getValue()+"</li>\n";
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
	
	
	public String toString(){
		return "DABFormatter";
	}
	
}
