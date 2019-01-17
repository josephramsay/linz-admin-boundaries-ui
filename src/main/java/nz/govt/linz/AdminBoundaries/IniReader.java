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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads windows style ini file see python ConfigParser
 * Code copied and adapted from https://stackoverflow.com/a/15638381
 * @author jramsay/aerospace
 *
 */
public class IniReader {
	
	private static final Logger LOGGER = Logger.getLogger( IniReader.class.getName() );

	private Pattern  section_p  = Pattern.compile( "\\s*\\[([^]]*)\\]\\s*" );
	private Pattern  option_p = Pattern.compile( "\\s*([^=]*)=(.*)" );
	private Pattern  value_p = Pattern.compile( "\\s+(.*)" );

	/** The entries array/map is the formatted copy of the config file */
	//            section     option  value
	protected Map<String, Map<String, String>> entries = new HashMap<>();

	private String path;

	/**
	 * Constructor setting path to ini file
	 * @param p
	 * @throws IOException
	 */
	public IniReader(String p) {
		LOGGER.finest("setpath "+p);
		setPath(p);
	}
	
	/**
	 * Instance method used for testing
	 * @param p
	 * @throws IOException
	 */
	public static IniReader getInstance(Map<String,Map<String,String>> e) {
		LOGGER.finest("unsaved instance");
		IniReader ir = new IniReader("");
		ir.setEntries(e);
		return ir;
	}

	/**
	 * Sets ini path
	 * @param p
	 */
	public void setPath(String p){
		path = p;
	}
	/**
	 * returns the ini file path
	 * @return
	 */
	public String getPath() {
		return path;
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
			if (save_flag){
				set(section,option,value.toString());
			}
		}
		catch (IOException ioe) {
			LOGGER.warning("Error reading config file, "+path+". "+ioe);
		}
		LOGGER.fine("e="+entries);
	}
	
	/**
	 * Sets path and reads the named ini file populating entries array
	 * @param p
	 * @throws IOException
	 */
	public void load(String p) {
		setPath(p);
		load();
	}

	/** Entries array getter. Returns the entries array in full */
	protected Map<String, Map<String, String>> getEntries(){
		return entries;
	}
	
	/** Entries array setter. Sets the entries array variable with a map-map */
	protected void setEntries(Map<String, Map<String, String>> entriesarg){
		entries = entriesarg;
	}

	//convenience methods to save having to fetch and re-save the entries array
	
	/** Sets and individual value in the entries array */
	public void setEntry(String sec,String opt, String val){
		if (!entries.containsKey(sec)) {entries.put(sec,new HashMap<String,String>());}
		entries.get(sec).put(opt, val);
	}

	/** Returns a single entry value from the entries array */
	public String getEntry(String sec,String opt){
		return entries.get(sec).get(opt);
	}	

	/** Returna the set of section elements */
	public Set<String> getSections(){
		return entries.keySet();
	}

	/** Returns the set of option elements for a particular section value */
	public Set<String> getOptions(String sec){
		return entries.get(sec).keySet();
	}


	/**
	 * Writes entries array back to its original file (NB comments and some formatting are lost)
	 * @throws IOException
	 */
	public void dump() throws IOException {
		try( BufferedWriter bw = new BufferedWriter( new FileWriter( path ))) {
			LOGGER.fine("e="+entries);
			for (String section : entries.keySet()){
				bw.write("["+section+"]\n");
				Map<String,String> detail = entries.get(section);
				for (String option : detail.keySet()){
					LOGGER.finer("s="+section+",o="+option+",v="+detail.get(option));
					bw.write(option+" = "+detail.get(option)+"\n");

				}
				bw.write("\n");
			} 
		}
	}
	
	/**
	 * Convenience dump overwriting entries before save
	 * @param entriesarg
	 * @throws IOException
	 */
	public void dump(Map<String,Map<String,String>> entriesarg) throws IOException {
		setEntries(entriesarg);
		dump();
	}

	/**
	 * flushes the contents of the config file
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public void flush() throws IOException {
		//(new PrintWriter(getPath())).close();//empties file only
		Files.deleteIfExists(Paths.get(getPath()));
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
		LOGGER.finer("s="+section+",o="+option+",v="+value);
		opt_val.put( option, value );
	}

	/**
	 * Alias for getentry (NB originally returned entries array itself...)
	 * @param section
	 * @param option
	 * @return
	 */	
	public String get( String section, String option ) {
		return getEntry(section,option);
	}
	
	/**
	 * Alias for getentry (NB originally returned entries array itself...)
	 * @param section
	 * @param option
	 * @param defaultvalue
	 * @return
	 */	
	public Object get( String section, String option, Object defaultvalue ) {
		Object value = getEntry(section,option);
		return value.equals(null) || value.toString().equals("") ? defaultvalue : value;
	}

}