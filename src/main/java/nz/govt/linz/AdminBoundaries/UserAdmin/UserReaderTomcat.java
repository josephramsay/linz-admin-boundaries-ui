package nz.govt.linz.AdminBoundaries.UserAdmin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

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

import org.apache.catalina.realm.MessageDigestCredentialHandler;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import nz.govt.linz.AdminBoundaries.UserAdmin.UserTomcat.GSMethod;

public class UserReaderTomcat extends UserReader {
	
	private static final Logger LOGGER = Logger.getLogger( UserReaderTomcat.class.getName() );

	private static final String USRP = "/conf/tomcat-users.xml";
	private static final String catalina_base_path = System.getProperty( "catalina.base" );
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
	 * Adds a user entry to the user_list and saves the result
	 * @param user Username
	 * @param pass Password unencrypted
	 */
	public void addUser(String username, String password, String roles) {
		UserTomcat user = new UserTomcat();
		user.setUserName(username);
		user.setPassword(encrypt(password));
		user.setRoles(roles);
		//user_list.add(user);
		//saveUserList();
		addUser(user);
	}
	
	public void editUser(String uname, String password, String roles) {
		UserTomcat user = new UserTomcat();		
		user.setUserName(uname);
		user.setPassword(encrypt(uname));
		user.setRoles(roles);
		//user_list.add(user);
		editUser(user);
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
				LOGGER.info("removing "+n.getAttributes().item(2));
			}
			catch (DOMException de) {
				LOGGER.warning("UserReader save Failure. "+de.toString());
			}
		}
		//add back modified users
		for (User user : user_list) {
			Element new_element = user_doc.createElement("user");
			new_element.setAttribute("username",user.getUserName());
			new_element.setAttribute("password",((UserTomcat)user).getPassword());
			new_element.setAttribute("roles",((UserTomcat)user).getRoleStr());
			root_element.appendChild(new_element);
			user_doc.normalize();
			//root_element.insertBefore(new_element, refChild)  appendChild(new_element);
			LOGGER.info("appending "+user.getUserName());
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
	public List<User> readUserList(){
		List<User> new_user_list = new ArrayList<>();
		NodeList user_nl = user_doc.getDocumentElement().getElementsByTagName("user");
		for (int i = 0; i < user_nl.getLength(); i++) {
			Node n = user_nl.item(i);
			User user = new UserTomcat();
			for (String upr : UserReader.getNames(GSMethod.class)) {
				user.writeUserAttribute(upr, n.getAttributes().getNamedItem(upr.toLowerCase()).getNodeValue());
			}
			new_user_list.add(user);
			//LOGGER.info("Read user "+user.getUserName());
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
		catch (ParserConfigurationException|SAXException|IOException multi) {
			System.err.println("UserReader Failure. "+multi.toString());
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
	
	@Override
	public List<User> cloneUserList() {
		List<User> new_user_list = new ArrayList<>();
		for (User user : user_list) {
			new_user_list.add(new UserTomcat((UserTomcat)user));
		}
		return new_user_list;
	}
	
	/** Simple tostring */
	public String toString(){
		String users = "";
		for (User user : user_list) {
			users += user.getUserName()+",";
		}
		return "UserReader::"+tomcat_filename+"\n"+users;
	}

}
