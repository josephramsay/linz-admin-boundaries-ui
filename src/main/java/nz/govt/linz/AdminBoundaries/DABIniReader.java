package nz.govt.linz.AdminBoundaries;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads windows style ini file see python ConfigParser
 * Code copied and adapted from https://stackoverflow.com/a/15638381
 * @author jramsay/aerospace
 *
 */
public class DABIniReader extends IniReader{

	//TODO relative path
	private final static String CONF_PATH = "/opt/apache-tomcat/webapps/ab/WEB-INF/scripts/download_admin_bdys.ini";
	//private final static String CONF_PATH = "WEB-INF/scripts/download_admin_bdys.ini";

	private Pattern file_p = Pattern.compile( "\\\"(\\w+)\\\"\\:\\{\\\"table\\\"" );
	private Pattern table_p = Pattern.compile( "\\\"table\\\"\\:\\\"(\\w+)\\\"" );
	private Pattern primary_p = Pattern.compile( "\\\"primary\\\"\\:\\\"(\\w+)\\\"" );

	//private Map<String, Map<String, String>> entries = new HashMap<>();
	private Map<String, Map<String,String>> colmap = new HashMap<>();

	/**
	 * Null constructor setting up on default confg path
	 * @throws IOException
	 */
	public DABIniReader() {
		this(CONF_PATH);
	}

	/**
	 * Constructor reads config file parsing meshblock and localities file blocks
	 * @param path
	 * @throws IOException
	 */
	public DABIniReader(String path) {
		super(path);
		load();
		parse(entries.get("meshblock").get("colmap"));
		parse(entries.get("nzlocalities").get("colmap"));
	}

	/**
	 * Limited parsing of JSON like strings fetching src/dst/primary/type for use in TableInfo constructor
	 * @return
	 */
	private void parse(String raw){
		Matcher file_m = file_p.matcher( raw );
		Matcher table_m = table_p.matcher( raw );
		Matcher primary_m = primary_p.matcher( raw );

		while (file_m.find() && table_m.find() && primary_m.find()){
			HashMap<String,String> entry = new HashMap<>();
			String tmp = file_m.group(1);
			String dst = table_m.group(1);
			String key = primary_m.group(1);
			entry.put("dst",dst);
			entry.put("tmp",tmp);
			entry.put("key",key);
			colmap.put(dst,entry);
		}
	}

	/**
	 * Returns dst/tmp/key triple for the active colmap 
	 * @param name
	 * @return
	 */
	public Map<String,String> getTriple(String name){
		return colmap.get(name);
	}


	public static void main(String[] args){
		String p = "testconfig.ini";
		try {
			DABIniReader reader1 = new DABIniReader(p);
			//Map<String, Map<String,String>> entries = reader1.getEntries();
			for (String sec : reader1.getSections()){
				for (String opt : reader1.getOptions(sec)){
					String val = reader1.getEntry(sec, opt);
					System.out.println(String.format("READ FROM >>> s=%s, o=%s, v=%s", sec,opt,val));

					if ("output_srid".equals(opt)){
						//increment val
						reader1.setEntry(sec, opt, Integer.toString(Integer.valueOf(val)+1));
					}
				}
			}
			//reader.setEntries(entries);
			reader1.dump();

			DABIniReader reader2 = new DABIniReader(p);
			//Map<String, Map<String,String>> entries2 = reader2.getEntries();
			for (String sec : reader2.getSections()){
				for (String opt : reader2.getOptions(sec)){
					String val = reader2.getEntry(sec, opt);
					System.out.println(String.format("WRITTEN TO >> s=%s, o=%s, v=%s", sec,opt,val));
				}
			}

		} 
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}