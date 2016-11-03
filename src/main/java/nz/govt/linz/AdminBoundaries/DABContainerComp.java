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

import java.io.*;
import java.util.*;


/**
 * Container for the list of tablemap objects and tablemap class as inner
 * @author jramsay
 * TODO. Better name
 */
public class DABContainerComp {
	
	//protected final static String[] TML = new String[]{"meshblock","meshblock_concordance","territorial_authority","nz_locality"};
	
	protected final static Map<String,String> TABV = new HashMap<>();
	static {
		TABV.put("meshblock","MB");
		TABV.put("meshblock_concordance","MBC");
		TABV.put("territorial_authority","TA");
		TABV.put("nz_locality","NZL");
	}
	
	protected final static String DEF_TABLE = "<table><caption>no table</caption>"
			+"<thead><tr><th><i>dabs</i></th></tr></thead>"
			+"<tbody><tr><td><i>data unavailable</i></td></tr></tbody></table>";
	
	/**
	 * Enum representing stages of import
	 * @author jramsay
	 */
	public enum ImportStatus {BLANK,LOADED,COMPLETE}
		
	//<inner> --------
	/**
	 * Container for a set of table mappings, emulating an enum but read from file
	 * @author jramsay
	 *
	 */
	public class TableInfo {
		private String dst; 
		private String tmp;
		private String key;
		private String abv;
		public TableInfo(String dst, String tmp, String key){
			this.dst = dst;
			this.tmp = "temp_"+tmp;
			this.key = key;
			this.abv = TABV.get(dst);
		}
		public String dst(){return dst;}
		public String tmp(){return tmp;}
		public String key(){return key;}
		public String abv(){return abv;}
		
		public String ttl(ImportStatus is){
			switch (is) {
			case BLANK: return "Import: "+tmp+"("+key+") &larr; SFTP";
			case LOADED: return "Transfer: "+dst+"("+key+") &larr; "+tmp+"("+key+")";
			case COMPLETE: return "Complete: "+dst+"("+key+") == "+tmp+"("+key+")";
			default: return "Load: "+dst+" Unavailable";
			}
		}
		//get table name to display on right of summary
		public String dsp(ImportStatus status){
		switch (status) {
			case BLANK: return DEF_TABLE;
			case LOADED: return tmp();
			case COMPLETE: return tmp();
			default: return "";
			} 
		}
		public String toString(){return "TableMap:"+String.join("/", dst,tmp,key);}
	}
	//</inner>------------------------------
	
	private Map<String,TableInfo> ti_map;
	private DABIniReader reader;
	
	public DABContainerComp(){
		ti_map = new HashMap<>();
		readConfig(new ArrayList<TableInfo>());
	}
	
	private void readConfig(List<TableInfo> tablemap){
		try {
			reader = new DABIniReader();
			initTMI();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	protected Map<String, Map<String, String>> getConfig(){
		return reader.getEntries();
	}
	protected void setConfig(Map<String, Map<String, String>> config){
		reader.setEntries(config);
	}
	
	
	/**
	 * Instantiate a TableMap from config values
	 * @param tm_name
	 * @return
	 */
	private TableInfo getTMInstance(String tm_name){
		Map<String,String> triple = reader.getTriple(tm_name);
		return new TableInfo(triple.get("dst"),triple.get("tmp"),triple.get("key"));
	}
	
	private void initTMI(){
		for (String tm_name : TABV.keySet()){
			ti_map.put(tm_name,getTMInstance(tm_name));
		}
	}
	/**
	 * Public accessor method
	 * @param i
	 */
	public TableInfo getTM(int i){
		return ti_map.get(i);
	}
	public Collection<TableInfo> values(){
		return ti_map.values();
	}
	public TableInfo valueOf(String tm_str){
		return ti_map.get(tm_str);
	}
	public TableInfo keyOf(String val){
		for (Object o : TABV.keySet()) {
            if (TABV.get(o).equals(val)) {
              return ti_map.get((String) o);
            }
        }
		return null;
	}
}