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

	//private static final Logger LOGGER = Logger.getLogger( DABIniReader.class.getName() );
	
	private Pattern file_p = Pattern.compile( "\\\"(\\w+)\\\"\\s*:\\s*\\{\\s*\\\"table\\\"" );
	private Pattern table_p = Pattern.compile( "\\\"table\\\"\\s*:\\s*\\\"(\\w+)\\\"" );
	private Pattern primary_p = Pattern.compile( "\\\"primary\\\"\\s*:\\s*\\\"(\\w+)\\\"" );

	//private Map<String, Map<String, String>> entries = new HashMap<>();
	private Map<String, Map<String,String>> colmap = new HashMap<>();

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
	 * Alternate string breaking for parsing JSON like strings 
	 * fetching src/dst/primary/type for use in TableInfo constructor
	 * @return
	 */
	private void parse2(String raw){
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

	/**
	 * main method just for testing
	 * @param args
	 * TODO delete this method
	 */
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