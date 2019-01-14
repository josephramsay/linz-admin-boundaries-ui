package nz.govt.linz.AdminBoundaries;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.catalina.CredentialHandler;
import org.apache.catalina.realm.MessageDigestCredentialHandler;
import org.apache.catalina.realm.UserDatabaseRealm;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class UserReaderTomcat extends UserReader {
	
	private static final String USRP = "conf/tomcat-users.xml";
	private static final String catalina_base_path = System.getProperty( "catalina.base" );
	private static final String test_base_path = "..";
	//private static final File catalina_base = new File( catalina_base_path ).getAbsoluteFile();
	
	public static final String ALG = "SHA-256";
	public static final int SALT = 0;
	public static final int ITER = 0;
	public static final Charset ENC = StandardCharsets.UTF_8;
	
	private String tomcat_filename;
	private Document user_doc;	

	/**
	 * Null constructor using default config file location 
	 */
	public UserReaderTomcat(){
		this(catalina_base_path+USRP);
	}

	/**
	 * Constructor sets up config file path and tests accessibility
	 * @param procarg
	 */
	public UserReaderTomcat(String _tomcat_filename){
		tomcat_filename =  _tomcat_filename;
		load();
	}

	/**
	 * Load the tomcat-users file into local doc object and read a map of the user entries
	 * @param tomcat_file File object for tomcat-users.xml
	 */
	@Override
	public void load() {
		File tomcat_file = new File(tomcat_filename);
		if (tomcat_file.canRead()) {
			user_doc = readTomcatFile(tomcat_file);
			user_list = readUserList();
		}
	}
	
	/**
	 * Save the user_doc back to the tomcat-users file
	 */
	@Override
	public void save() {
		Transformer transformer;
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	
			//initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new File(tomcat_filename));
			DOMSource source = new DOMSource(user_doc);
			transformer.transform(source, result);
		} catch (TransformerFactoryConfigurationError | TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}




	


	/**
	 * Rewrites the user_doc by deleting all existing users and replacing them with the users saved in the user_list
	 */
	@Override
	public void saveUserList() {
		Element root_element = user_doc.getDocumentElement();
		//Delete all existing users
		NodeList user_nl = root_element.getElementsByTagName("user");
		int user_len = user_nl.getLength();
		for (int i = 0; i < user_len; i++) {
			Node n = user_nl.item(0);
			try {
				root_element.removeChild(n);
				System.out.println("removing "+n.getAttributes().item(2));
			}
			catch (DOMException de) {
				System.err.println("UserReader save Failure. "+de.toString());
			}
		}
		//add back modified users
		for (Map<String, String> entry : user_list) {
			Element new_element = user_doc.createElement("user");
			new_element.setAttribute("username",entry.get("username"));
			new_element.setAttribute("password",entry.get("password"));
			new_element.setAttribute("roles",entry.get("roles"));
			root_element.appendChild(new_element);
			user_doc.normalize();
			//root_element.insertBefore(new_element, refChild)  appendChild(new_element);
			System.out.println("appending "+entry.get("username"));
		}
		//Tidy up by removing blank lines
		int i=0;
        while (root_element.getChildNodes().item(i)!=null) {
            if (root_element.getChildNodes().item(i).getNodeName().equalsIgnoreCase("#text")) {
                root_element.removeChild(root_element.getChildNodes().item(i));
            }
            i=i+1;

        }
	}
	
	/**
	 * Parses tomcat users file returning map of user/pass entries
	 * @param doc Document object of tomcat-users file
	 * @return HashMap of user/password pairs
	 */
	@Override
	public List<Map<String,String>> readUserList(){
		List<Map<String,String>> new_user_list = new ArrayList<>();
		NodeList user_nl = user_doc.getDocumentElement().getElementsByTagName("user");
		for (int i = 0; i < user_nl.getLength(); i++) {
			Node n = user_nl.item(i);
			Map<String,String> user_entry = new HashMap<>();
			for (String upr : Arrays.asList("username","password","roles")) {
				user_entry.put(upr, n.getAttributes().getNamedItem(upr).getNodeValue());
			}
			new_user_list.add(user_entry);
			System.out.println("READ - "+user_entry.get("username"));
		}
		return new_user_list;
	}

	/**
	 * Initialises ProcessBuilder returning output from requested script 
	 * @param arg Argument to the aims_extract.sh script indicating transfer, process or reject
	 * @return
	 */
	public Document readTomcatFile(File tomcat_file){
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();
			Document user_doc = dbBuilder.parse(tomcat_file);
			return user_doc;
		} 
		catch (ParserConfigurationException pce){
			System.err.println("UserReader Failure. "+pce.toString());
		}
		catch (SAXException se){
			System.err.println("UserReader Failure. "+se.toString());
		}
		catch (IOException ioe) {
			System.err.println("UserReader Failure. "+ioe.toString());
		}
		return null;
	}

	
	/**
	 * Returns a hash of the provided text string
	 * Command to get pw is: "./digest.sh -a SHA-256 -s 0 $p | cut -d: -f2`"
	 * @param plain Plain text string
	 * @return Hash of plaintext unless in error in which case returns the plaintext unaltered
	 */
	@Override
	public String encrypt(String plain) {
		try {
			MessageDigestCredentialHandler handler = new MessageDigestCredentialHandler();
			handler.setAlgorithm(UserReaderTomcat.ALG);
			handler.setIterations(UserReaderTomcat.ITER);
			handler.setSaltLength(UserReaderTomcat.SALT);
			handler.setEncoding(UserReaderTomcat.ENC.name());
			return handler.mutate(plain); 
		} catch (NoSuchAlgorithmException nsae) {
			nsae.printStackTrace();
		}
		return plain;
	}
	/** Simple tostring */
	public String toString(){
		String users = "";
		for (Map<String,String> ul : user_list) {
			users += ul.get("username")+",";
		}
		return "UserReader::"+tomcat_filename+"\n"+users;
	}

}
