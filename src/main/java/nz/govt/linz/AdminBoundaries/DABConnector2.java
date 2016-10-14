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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Connector intermediate class handles database connectivity and file read/write. Also does minimal post processing
 * @author jramsay
 *
 */
public class DABConnector2 {
	
	//private Connector connector;
	DataSource datasource = null;
	
	/**
	 * Constructor for DAB database connector
	 */
	public DABConnector2() {			
		try {
			datasource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/linz/aims");
		}
		catch (NamingException ne){
			System.out.println(ne);
		}
	}
	
	/**
	 * Reads a named properties file using resourceloader class returning a map
	 * @param fname
	 * @return
	 */
	public Map<String,String> readParameters(String fname){
		Map<String, String> params = new HashMap<>();
		Properties prop = ResourceLoader.getAsProperties(fname);
		Set<String> propnames = prop.stringPropertyNames();
		for (String p : propnames){
			params.put(p, prop.getProperty(p));
		}
		return params;
	}	
	
	/**
	 * readConfig reads configproperties-like flat file 
	 * @param fname
	 * @return
	 */
	public Map<String,String> readConfig(String fname){
		String delim = "[=:]";
		Map<String, String> params = new HashMap<>();
		try(BufferedReader br = new BufferedReader(new FileReader(fname))) {
		    String line = br.readLine();
		    while (line != null) {
		    	String[] kv = line.split(delim);
		        params.put(kv[0], kv[1]);
		        line = br.readLine();
		    }
		}		
		catch (FileNotFoundException fnfe){
			fnfe.printStackTrace();
		}
		catch (IOException ioe){
			ioe.printStackTrace();
		}

		return params;
	}
	
	/**
	 * Fetches summary data from temp import schema, admin_bdys_import
	 * @return List<List<String>> representing a table
	 */
	public List<List<String>> executeQuery(String query){
		List<List<String>> result = null;
		//System.out.println(String.format("### %s + %s", prefix,suffix));
		try {
			ResultSet rs = exeQuery(query);
			result = parseResultSet(rs);
		}
		catch (SQLException sqle) {
			result = parseSQLException(sqle);
		}
		return result;
	}
	
	//TODO something less ugly
	public boolean executeTFQuery(String query){
		boolean result = false;
		try {
			ResultSet rs = exeQuery(query);
			if (rs.next()){
				result = rs.getBoolean(1);
			}
		}
		catch (SQLException sqle) {
			System.out.println("SQLError "+sqle);
		}
		return result;
	}		
	
	public String executeSTRQuery(String query){
		String result = "";
		try {
			ResultSet rs = exeQuery(query);
			if (rs.next()){
				result = rs.getString(1);
			}
		}
		catch (SQLException sqle) {
			System.out.println("SQLError "+sqle);
		}
		return result;
	}	
	
	/**
	 * Local query wrapper
	 * @param query
	 * @return
	 * @throws SQLException
	 */
	private ResultSet exeQuery(String query) throws SQLException {
		ResultSet result = null;
		try (Connection conn = datasource.getConnection()){
			Statement stmt = conn.createStatement();
			result = stmt.executeQuery(query);			
		}
		return result;
	}
	
	
	
	/**
	 * Generic resultset-table to list-list formatter
	 * @param rs
	 * @return
	 */
	private List<List<String>> parseResultSet(ResultSet rs) throws SQLException {
		List<String> row;// = new ArrayList<String>();
		List<List<String>> table = new ArrayList<>();
		
	    ResultSetMetaData rsmd = rs.getMetaData();
	    int count = rsmd.getColumnCount();
	    row = new ArrayList<>();
	    for (int i=1; i<=count; i++) {
	    	row.add(rsmd.getColumnLabel(i));
	    }
	    table.add(row);
	    while (rs.next()) {
	    	row = new ArrayList<>();
	    	for (int i=1; i<=count; i++) {
	    		row.add(rs.getString(i));
	    	}
	    	table.add(row);
		}
		return table;
	}
	
	private List<List<String>> parseSQLException(SQLException sqle){		
		List<List<String>> result = null;	
		//Error is written to general log and result is returned
		System.out.println("SQL error "+sqle);
		//return the error to the user
		result = new ArrayList<>();
		List<String> line = new ArrayList<>();
		line.add("SQLException");
		line.add(sqle.toString());
		result.add(line);
		return result;
	}
	
	
	public String toString(){
		return "DABConnector::";//+connector;
	}
	
	/**
	 * main method used for testing
	 * @param args
	 */
	public static void main(String[] args){
		DABConnector2 dabc = new DABConnector2();
		//System.out.println(dabc.readConfig(FILENAME));
		//System.out.println(dabc.readProps(FILENAME));
		System.out.println(dabc.executeQuery("select 1"));
		

	}
	
}
