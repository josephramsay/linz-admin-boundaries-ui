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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import org.apache.catalina.realm.MessageDigestCredentialHandler;
import org.apache.tomcat.util.res.StringManager;
import org.apache.juli.logging.LogFactory;


/**
 * Class to initiate and run system processes, n this case the included python script, download_admin_bdys.py
 * @author jramsay
 *
 */
public class UserReader {

	//this is the path where the debian packager puts the py part of the app
	private static final String USRP = "conf/tomcat-users.xml";
	private static final String catalina_base_path = System.getProperty( "catalina.base" );
	private static final String test_base_path = "..";
	//private static final File catalina_base = new File( catalina_base_path ).getAbsoluteFile();
	
	private String tomcat_filename;
	private Document user_doc;
	private List<Map<String,String>> user_list;

	public static String ALG = "SHA-256";
	public static int SALT = 0;
	public static int ITER = 0;
	public static Charset ENC = StandardCharsets.UTF_8;


	/**
	 * Null constructor using default config file location 
	 */
	public UserReader(){
		this(new File(catalina_base_path, USRP));
	}

	/**
	 * Constructor sets up config file path and tests accessibility
	 * @param procarg
	 */
	public UserReader(File tomcat_file){
		tomcat_filename =  tomcat_file.getAbsolutePath();
		if ( tomcat_file.canRead()) {
			loadFile(tomcat_file);
		}
	}

	/**
	 * Load the tomcat-users file into local doc object and read a map of the user entries
	 * @param tomcat_file File object for tomcat-users.xml
	 */
	private void loadFile(File tomcat_file) {
		user_doc = readTomcatFile(tomcat_file);
		user_list = readUserSection();
	}
	
	/**
	 * Save the user_doc back to the tomcat-users file
	 * @throws TransformerFactoryConfigurationError
	 * @throws TransformerException
	 */
	public void saveFile() throws TransformerFactoryConfigurationError, TransformerException {
		Transformer transformer = TransformerFactory.newInstance().newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		//initialize StreamResult with File object to save to file
		StreamResult result = new StreamResult(new File(tomcat_filename));
		DOMSource source = new DOMSource(user_doc);
		transformer.transform(source, result);
	}

	/**
	 * Convenience getter for the user map 
	 * @return Map of users and passwords
	 */
	public List<Map<String,String>> getUserList(){
		return user_list;
	}

	/**
	 * Returns a hash of the provided text string
	 * Command to get pw is: "./digest.sh -a SHA-256 -s 0 $p | cut -d: -f2`"
	 * @param plain Plain text string
	 * @return Hash of plaintext unless in error in which case returns the plaintext unaltered
	 */
	public static String encrypt(String plain) {
		try {
			MessageDigestCredentialHandler handler = new MessageDigestCredentialHandler();
			handler.setAlgorithm(UserReader.ALG);
			handler.setIterations(UserReader.ITER);
			handler.setSaltLength(UserReader.SALT);
			handler.setEncoding(UserReader.ENC.name());
			String hash = handler.mutate(plain); 
			return hash;
		} catch (NoSuchAlgorithmException nsae) {
			nsae.printStackTrace();
		}
//		return "###"+plain+"###";
//		try {
//			MessageDigest digest = MessageDigest.getInstance(UserReader.ALG);
//			byte[] bytes = digest.digest(plain.getBytes(UserReader.ENC));
//			String hash = new String(bytes, UserReader.ENC);
//			//System.out.println(plain+" -> "+hash);
//			return hash;
//		} catch (NoSuchAlgorithmException nsae) {
//			nsae.printStackTrace();
//		}
		return plain;
	}

	/**
	 * Adds a user pass entry to the user_list and saves the result
	 * @param user Username
	 * @param pass Password unencrypted
	 */
	public void addUser(String username,String password, String roles) {
		Map<String,String> user_entry = new HashMap<>();
		user_entry.put("username", username);
		user_entry.put("password", encrypt(password));
		user_entry.put("roles", roles);
		user_list.add(user_entry);
		saveUserSection();
	}
	
	/**
	 * Removes the selected user from the user_list and saves
	 * @param user Username key
	 */
	public void delUser(String username) {
		user_list.remove(findInUserList(username));
		saveUserSection();
	}
	/**
	 * Replaces the user/pass combo in the user_list and saves.
	 * Only adds a replacement if the original exists
	 * @param user Username
	 * @param pass Password unencrypted
	 */
	public void editUser(String username,String password, String roles) {
		if (user_list.remove(findInUserList(username))) {
			addUser(username, password, roles);
		}
	}
	
	/**
	 * Returns a matching user_list entry matching by provided key/value pair
	 * @param key
	 * @param value
	 * @return User list entry
	 */
	private Map<String,String> findInUserList(String key, String value){
		for (Map<String,String> user_entry : user_list){
			if (Objects.equals(user_entry.get(key),value)) {
				return user_entry;
			}
		}
		return null;
	}
	
	/**
	 * Returns a matching user_list entry matching by provided username
	 * @param username
	 * @return
	 */
	public Map<String,String> findInUserList(String value){
		return findInUserList("username", value);
	}

	/**
	 * Rewrites the user_doc by deleting all existing users and replacing them with the users saved in the user_list
	 */
	private void saveUserSection() {
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
	public List<Map<String,String>> readUserSection(){
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

	/** Simple tostring */
	public String toString(){
		String users = "";
		for (Map<String,String> ul : user_list) {
			users += ul.get("username")+",";
		}
		return "UserReader::"+tomcat_filename+"\n"+users;
	}

}

