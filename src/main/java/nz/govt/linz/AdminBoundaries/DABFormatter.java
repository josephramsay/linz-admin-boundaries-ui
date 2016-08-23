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


public class DABFormatter {
	
	/**
	 * Creates out put formatted text from table data
	 */

	
	public String getSummaryAsTable(String tname, List<List<String>> result) {
		
		String table = "";
	    table += "<table>";
	    table += "<caption>"+tname+"</caption>";
	    table += "<tr>";
	    List<String> head = result.get(0);
	    for (String cell : head) {
	    	table += "<th>" +  cell + "</th>";
	    }
	    table += "</tr>";
    	for (int i=1; i<result.size(); i++) {
    		table += "<tr>";
    		List<String> row = result.get(i);
    		for (String cell : row){
    			table += "<td>" + cell + "</td>";
    		}
    		table += "</tr>";
	    }

	    table += "</table>";
	    return table;
	}

	
	
	public String toString(){
		return "DABFormatter";
	}
	
}
