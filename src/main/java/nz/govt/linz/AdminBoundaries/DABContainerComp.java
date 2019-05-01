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
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;


/**
 * Container for the list of tablemap objects and tablemap class as inner
 * @author jramsay
 * TODO. Better name
 */
public class DABContainerComp {
	
	private static final Logger LOGGER = Logger.getLogger( DABContainerComp.class.getName() );
	
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
	 */
	public class TableInfo {
		private String dst; 
		private String tmp;
		private String key;
		private String abv;
		public TableInfo(String dst, String tmp, String key){
			this.dst = dst;
			this.tmp = reader.get("database", "prefix")+tmp;
			this.key = key;
			this.abv = TABV.get(dst);
		}
		public String dst(){return dst;} //name of destination table
		public String tmp(){return tmp;} //name of temporary (import) table
		public String key(){return key;} //primary key
		public String abv(){return abv;} //table name abbreviation
		
		//get message to display showing import status (beside compare function)
		public String ttl(ImportStatus is){
			if (is == null){return "Load: "+dst+" Unavailable";}
			else {
				switch (is) {
				case BLANK: return "Import: "+tmp+"("+key+") &larr; WFS/SFTP";
				case LOADED: return "Transfer: "+dst+"("+key+") &larr; "+tmp+"("+key+")";
				case COMPLETE: return "Complete: "+dst+"("+key+") == "+tmp+"("+key+")";
				default: return "Load: "+dst+" Unavailable";
				}
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
	
	/** Map of destination table names to TableInfo enum */
	private Map<String,TableInfo> ti_map;
	private DABIniReader reader;
	
	
	//-------------------------------------------------------------------------
	
	/**
	 * Main constructor, sets up table info and config reader
	 * @param conf
	 */
	public DABContainerComp(DABIniReader reader) {
		setTIMap(new HashMap<>());
		setReader(reader);
		initTMI();
	}
	
	/** Sets TableInfo map */
	public void setTIMap(HashMap<String,TableInfo> tim){
		ti_map = tim;
	}
	
	/** Sets DABIniReader object */
	public void setReader(DABIniReader ir){
		reader = ir;
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Wrapper for fetching entries array
	 * @return
	 */
	protected Map<String, Map<String, String>> getConfig(){
		Map<String, Map<String, String>> config = reader.getEntries();
		LOGGER.fine("Fetching Config "+config.toString());
		return config;
	}
	protected Map<String, String[]> getConfigDescriptions(){
		return DABIniReader.getDescriptions();
	}
	
	/**
	 * Wrapper to set entries array and trigger file write
	 * @param config
	 */
	protected void setConfig(Map<String, Map<String, String>> config) {
		try {
			LOGGER.fine("Setting Config "+config.toString());
			reader.setEntries(config);
			reader.dump();		
		} catch (IOException ioe) {
			// TODO Auto-generated catch block
			ioe.printStackTrace();
		}
		initTMI();
	}
	
	
	/**
	 * Instantiate a TableMap from config values
	 * @param tm_name
	 * @return
	 */
	private TableInfo getTIInstance(String tm_name){
		Map<String,String> triple = reader.getTriple(tm_name);
		return new TableInfo(triple.get("dst"),triple.get("tmp"),triple.get("key"));
	}
	
	/**
	 * Initialises the TI map with new TI instances
	 */
	private void initTMI(){
		for (String tm_name : TABV.keySet()){
			ti_map.put(tm_name,getTIInstance(tm_name));
		}
	}
	
	
	/**
	 * getter for TI map values
	 * @return
	 */
	public Collection<TableInfo> values(){
		return ti_map.values();
	}
	
	/**
	 * getter for named TI values
	 * @param tm_str
	 * @return
	 */
	public TableInfo valueOf(String tm_str){
		return ti_map.get(tm_str);
	}
	
	/**
	 * Reverse lookup abbreviation value->key for the TI map
	 * @param abv_val
	 * @return
	 */
	public TableInfo keyOf(String abv_val){
		for (Object o : TABV.keySet()) {
            if (TABV.get(o).equals(abv_val)) {
              return ti_map.get((String) o);
            }
        }
		return null;
	}
}