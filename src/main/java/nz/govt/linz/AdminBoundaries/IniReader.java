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
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads windows style ini file see python ConfigParser
 * Code copied and adapted from https://stackoverflow.com/a/15638381
 * @author jramsay/aerospace
 *
 */
public class IniReader {

	private Pattern  section_p  = Pattern.compile( "\\s*\\[([^]]*)\\]\\s*" );
	private Pattern  option_p = Pattern.compile( "\\s*([^=]*)=(.*)" );
	private Pattern  value_p = Pattern.compile( "\\s+(.*)" );

	protected Map<String, Map<String, String>> entries = new HashMap<>();

	private String path;

	/**
	 * Constructor setting path to ini file
	 * @param p
	 * @throws IOException
	 */
	public IniReader(String p) {
		setPath(p);
	}

	/**
	 * Sets ini path
	 * @param p
	 */
	public void setPath(String p){
		path = p;
	}

	/**
	 * Reads ini file populating entries array
	 * @throws IOException
	 */
	public void load() {
		try( BufferedReader br = new BufferedReader( new FileReader( path ))) {
			boolean save_flag = false;
			String line;
			String section = null;
			String option = null;
			StringBuilder value = new StringBuilder();
			while(( line = br.readLine()) != null ) {
				//match [section]
				Matcher section_m = section_p.matcher( line );
				if( section_m.matches()) {
					if (save_flag){
						set(section,option,value.toString());
						value.setLength(0);
						save_flag = false;
					}
					section = section_m.group(1).trim();
				}
				//match option=value 
				Matcher option_m = option_p.matcher( line );
				if( option_m.matches()) {
					if (save_flag){
						set(section,option,value.toString());
						value.setLength(0);
					}
					option = option_m.group(1).trim();
					value.append(option_m.group(2).trim());
					save_flag = true;
				}
				//match. matches line following section/option and appends to current option value
				Matcher value_m = value_p.matcher( line );
				if( value_m.matches()) {
					value.append(value_m.group(1).trim());
				}
			}
			if (save_flag){set(section,option,value.toString());}
		}
		catch (IOException ioe) {
			System.out.println("Error reading config file, "+path+". "+ioe);;
		}
	}

	/** Entries array getter */
	protected Map<String, Map<String, String>> getEntries(){
		return entries;
	}
	
	/** Entries array setter */
	protected void setEntries(Map<String, Map<String, String>> entriesarg){
		entries = entriesarg;
	}

	//convenience methods to save having to fetch and re-save the entries array
	
	/** Sets and individual value in the entries array */
	protected void setEntry(String sec,String opt, String val){
		entries.get(sec).put(opt, val);
	}

	/** Returns a single entry value from the entries array */
	protected String getEntry(String sec,String opt){
		return entries.get(sec).get(opt);
	}	

	/** Returna the set of section elements */
	protected Set<String> getSections(){
		return entries.keySet();
	}

	/** Returns the set of option elements for a particular section value */
	protected Set<String> getOptions(String sec){
		return entries.get(sec).keySet();
	}

	//------------------------------------------------

	/**
	 * Writes entries array back to its original file (NB comments and some formatting are lost)
	 * @throws IOException
	 */
	public void dump() throws IOException {
		try( BufferedWriter bw = new BufferedWriter( new FileWriter( path ))) {
			for (String section : entries.keySet()){
				bw.write("["+section+"]\n");
				Map<String,String> detail = entries.get(section);
				for (String option : detail.keySet()){
					bw.write(option+" = "+detail.get(option)+"\n");

				}
				bw.write("\n");
			} 
		}
	}

	/**
	 * Set entries Section, Option, Value
	 * @param section
	 * @param option
	 * @param value
	 */
	private void set(String section, String option, String value){
		Map< String, String > opt_val = entries.get(section);
		if( opt_val == null ) {
			entries.put( section, opt_val = new HashMap<>());   
		}
		opt_val.put( option, value );
	}

	/**
	 * Alias for getentry (NB originally returned entries array itself...)
	 * @param section
	 * @param option
	 * @param value
	 * @return
	 */
	public String get( String section, String option, String value) {
		return getEntry(section,option);
	}

	public String getString( String section, String option, String defaultvalue ) {
		Map< String, String > opt_val = entries.get( section );
		if( opt_val == null ) {
			return defaultvalue;
		}
		return opt_val.get( option );
	}

	public int getInt( String section, String option, int defaultvalue ) {
		Map< String, String > opt_val = entries.get( section );
		if( opt_val == null ) {
			return defaultvalue;
		}
		return Integer.parseInt( opt_val.get( option ));
	}

	public float getFloat( String section, String option, float defaultvalue ) {
		Map< String, String > opt_val = entries.get( section );
		if( opt_val == null ) {
			return defaultvalue;
		}
		return Float.parseFloat( opt_val.get( option ));
	}

	public double getDouble( String section, String option, double defaultvalue ) {
		Map< String, String > opt_val = entries.get( section );
		if( opt_val == null ) {
			return defaultvalue;
		}
		return Double.parseDouble( opt_val.get( option ));
	}
}