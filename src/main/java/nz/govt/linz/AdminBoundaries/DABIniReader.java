package nz.govt.linz.AdminBoundaries;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads windows style ini file see python ConfigParser
 * @author jramsay
 *
 */
public class DABIniReader extends IniReader{

	@SuppressWarnings("unused")
	private static final Logger LOGGER = Logger.getLogger( DABIniReader.class.getName() );
	
	private Pattern file_p = Pattern.compile( "\\\"(\\w+)\\\"\\s*:\\s*\\{\\s*\\\"table\\\"" );
	private Pattern table_p = Pattern.compile( "\\\"table\\\"\\s*:\\s*\\\"(\\w+)\\\"" );
	private Pattern primary_p = Pattern.compile( "\\\"primary\\\"\\s*:\\s*\\\"(\\w+)\\\"" );

	//private Map<String, Map<String, String>> entries = new HashMap<>();
	private Map<String, Map<String,String>> colmap = new HashMap<>();

	/** The descriptions array is the a map of sections and some descriptive text 
	 * Its hardcoded here (for now) since it doesn't belong in the config file 
	 * TODO. Push to properties file and add a default/placeholder for when a changed
	 *  config file doesn't match any description */
	protected static Map<String, String[]> descriptions = new HashMap<>();

	/** 
	 * Get field descriptions for config form
	 * @return
	 */
	public static Map<String, String[]> getDescriptions() {
		return descriptions;
	}
	
	/**
	 * Sets the field descriptions for the config form
	 */
	private static void setDescriptions() {
		descriptions.put("database", new String[] {
			"host - the database host name (can be omitted)",
			"name - the Database name (linz_db)",
			"rolename - the User to run downloader functions as (this user will own the created tables) (bde_dba)",
			"user - the user to login as (must be a member of unix_logins) (dab_user)",
			"password - login user password",
			"port - database port (5432)",
			"schema - temporary schema where imported tables are stored (admin_bdys_import)",
			"originschema - schema where admin boundaries are saved (admin_bdys)",
			"prefix - prefix to prepend to table names in the import schema (temp_)"});
		descriptions.put("api", new String[] {
			"host - API host",
			"user - DAB included username",
			"password - DAB included user password"});
		descriptions.put("ldswfs", new String[] {
			"version - WFS version for URL parameter (2.0.0)",
			"host - WFS host (LDS)",
			"key - LDS API key"});
		descriptions.put("statswfs", new String[] {
				"version - WFS version for URL parameter (2.0.0)",
				"host - WFS host (stats)",
				"key - Stats API key"});
		descriptions.put("user", new String[] {
			"list - comma seperated list of usernames that will be notified when import completes",
			"domain - email list domain name (linz.govt.nz)",
			"smtp - SMTP server",
			"link - link provided to users in notification message (can be omitted)",
			"clargs - optional arguments provided to process script"});
		descriptions.put("layer", new String[] {
			"name - name of table to apply grid function to (territorial_authority)",
			"output_srid - SRID to transfor imported layers to (4167)",
			"geom_column - grid able geom column name (shape)",
			"create_grid - Enable grid creation T/F (True)",
			"grid_res - Grid resolution (0.05)",
			"shift_geometry - enable geom shift function. <i>needed to include cross dateline locations</i> (True)"});
		descriptions.put("connection", new String[] {
			"ftphost - FTP host to connect to for StatsNZ data",
			"ftpport - FTP port (22)",
			"ftpuser - FTP login/user",
			"ftppass - FTP password",
			"ftppath - Path from FTP root to search for files"});
		descriptions.put("meshblock", new String[] {
			"filepattern - Regular expression matching meshblock file",
			"localpath - Temporary path to save downloaded files (/tmp)",
			"colmap - list of table reformatting rules <i>see Notes below</i>"});
		descriptions.put("nzlocalities", new String[] {
			"filepath - (path to localities share)",
			"filename - (name of localities file)",
			"colmap - list of table reformatting rules <i>see Notes below</i>"});
		descriptions.put("optional", new String[] {
			"functions - AB related functions to be called during scheduled/optional run"});
		descriptions.put("validation", new String[] {
			"test - [[(test query) , (expected result)],[]],",
			"data - [[(test query) , (expected result)],[]],",
			"spatial - [[(test query) , (expected result)],[]]",});
		descriptions.put("colmap", new String[] {
			"( file1 ) : ",
			"&emsp;{ table1 : ( tablename ),",
			"&emsp;&emsp;rename : [ { old : ( old column name ), new : ( new column name ) }, {} ],",
			"&emsp;&emsp;add : [ { add : ( column to add ), type : ( type of added column ) } ],",
			"&emsp;&emsp;drop : [ ( column to drop ), () ],",
			"&emsp;&emsp;primary : ( primary key column name ),",
			"&emsp;&emsp;geom : ( geometry column name ),",
			"&emsp;&emsp;srid : ( spatial reference ),",
			"&emsp;&emsp;grid : { geocol : (geometry colum name ), res : ( grid resolution ) },",
			"&emsp;&emsp;cast : [ { cast: ( column name to re cast ), type : ( data type to cast column to ) }, {} ],",
			"&emsp;&emsp;permission : [(user with schema access permissions) , () ]",
			"&emsp;},",
			"&emsp;{ table2 : ... },",
			"( file2 ) : {}"});
	}
	
	/**
	 * Constructor reads config file parsing meshblock and localities file blocks
	 * @param path
	 * @throws IOException
	 */
	public DABIniReader(String path) {
		super(path);
		setDescriptions();
		load();
		parse(entries.get("meshblock").get("colmap"));
		parse(entries.get("nzlocalities").get("colmap"));
	}

	/**
	 * Alternate string breaking for parsing JSON like strings 
	 * fetching src/dst/primary/type for use in TableInfo constructor
	 * @return
	 */
	@SuppressWarnings("unused")
	private void parse_alt(String raw){
		for (String segment : raw.split("},")) {
			Matcher file_m = file_p.matcher( segment );
			Matcher table_m = table_p.matcher( segment );
			Matcher primary_m = primary_p.matcher( segment );
			
			if (file_m.find() && table_m.find() && primary_m.find()){
				popColmap(new Matcher[]{file_m,table_m,primary_m});
			}
		}
	}
	
	/**
	 * Limited parsing of JSON like strings fetching src/dst/primary/type 
	 * for use in TableInfo constructor
	 * @return
	 */
	private void parse(String raw){
		Matcher file_m = file_p.matcher( raw );
		Matcher table_m = table_p.matcher( raw );
		Matcher primary_m = primary_p.matcher( raw );

		while (file_m.find() && table_m.find() && primary_m.find()){
			popColmap(new Matcher[]{file_m,table_m,primary_m});
		}
	}
	
	/**
	 * Extracts group1 fields from matched strings
	 * @param matchers file table and primary string matchers
	 */
	private void popColmap(Matcher[] matchers) {
		HashMap<String,String> entry = new HashMap<>();
		String tmp = matchers[0].group(1);
		String dst = matchers[1].group(1);
		String key = matchers[2].group(1);
		entry.put("dst",dst);
		entry.put("tmp",tmp);
		entry.put("key",key);
		colmap.put(dst,entry);
	}

	/**
	 * Returns dst/tmp/key triple for the active colmap 
	 * @param name
	 * @return
	 */
	public Map<String,String> getTriple(String name){
		return colmap.get(name);
	}

}