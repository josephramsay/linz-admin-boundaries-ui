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
import java.sql.DriverManager;
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

import nz.govt.linz.AdminBoundaries.DBConnection.ConnectionDefinitions;
//import nz.govt.linz.AdminBoundaries.DBConnection.Connector;

public class DABConnector {
	
	//private Connector connector;
	final static String FILENAME = "postgresql.properties";//config.txt"
	
	/**
	 * Constructor for DAB database connector
	 */
	public DABConnector() {		
		//Map<String, String> params = readParameters(FILENAME);
		//connector = new PostgreSQLConnector();
		//connector.init(params);
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
	 * @return
	 */
	public  List<List<String>> getExtractSummary(String query){
		List<List<String>> result = null;
		//try (Connection conn = connector.getConnection()) {
		Properties props = ResourceLoader.getAsProperties(FILENAME);
		String prefix = ConnectionDefinitions.POSTGRESQL.prefix();
		String suffix = String.format("//%s:%s/%s", props.get("host"),props.get("port"),props.get("dbname"));
		try (Connection conn = DriverManager.getConnection(String.format("%s:%s",prefix,suffix),props)) {
			Statement stmt = conn.createStatement();
			result = parseResultSet(stmt.executeQuery(query));
		} 
		catch (SQLException sqle) {
			// TODO Auto-generated catch block
			sqle.printStackTrace();
		}
		return result;
	}
	
	/**
	 * Generic resultset-table to list-list formatter
	 * @param rs
	 * @return
	 */
	public List<List<String>> parseResultSet(ResultSet rs) throws SQLException {
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
	
	public String toString(){
		return "DABConnector::";//+connector;
	}
	
	public static void main(String[] args){
		DABConnector dabc = new DABConnector();
		//System.out.println(dabc.readConfig(FILENAME));
		//System.out.println(dabc.readProps(FILENAME));
		System.out.println(dabc.getExtractSummary("select 1"));
		

	}
	
}
