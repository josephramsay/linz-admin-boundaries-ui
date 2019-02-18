package nz.govt.linz.AdminBoundaries.UserAdmin;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import nz.govt.linz.AdminBoundaries.DABIniReader;
import nz.govt.linz.AdminBoundaries.UserAdmin.User.Action;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserAIMS.AARoles;

//import org.apache.catalina.realm.UserDatabaseRealm;
//
//import nz.co.espatial.aims.db.DBUtils;
//import nz.co.espatial.aims.db.DBUtils.QueryTransaction;
//import nz.co.espatial.aims.db.model.User;
//import nz.co.espatial.aims.db.reference.UserDao;
//import nz.co.espatial.aims.AIMS;

public class UserReaderAIMS extends UserReader {
	
	private static final Logger LOGGER = Logger.getLogger(UserReaderAIMS.class.getName());
	
	public static final String user_ref_base = "http://<SVR>:8080/aims/api/admin/users";
	
	/** Simple pair class for actions put/post and their json payloads */
	class ActionPayload {
		public String plus;
		public Action action;
		public JsonObject payload;
		ActionPayload(String plus,Action action, JsonObject payload){
			this.plus = plus;
			this.action = action;
			this.payload = payload;
		}
	}
	
	private String aims_url;
	private JsonObject json_conn;
	private List<User> user_list_clone;

	public UserReaderAIMS(DABIniReader reader){
		this(user_ref_base.replace("<SVR>",reader.get("api", "host")),
			reader.get("api", "user"),
			reader.get("api", "password"));
	}
	/**
	 * url constructor inits API access to aims user db
	 * @param procarg
	 */
	public UserReaderAIMS(String _aims_url, String u, String p){
		aims_url =  _aims_url;
		setDefAuth(u,p);
		load();
	}
	
	
	public static List<String> getOrgNames(){		
		return Stream.of(UserAIMS.Organisation.values())
			.map(x->x._name())
			.collect(Collectors.toList());
	}
	
	private void setDefAuth(String u, String p) {
		Authenticator.setDefault (new Authenticator() {
		    protected PasswordAuthentication getPasswordAuthentication() {
		        return new PasswordAuthentication (u,p.toCharArray());
		    }
		});
	}

	/**
	 * Load the tomcat-users file into local doc object and read a map of the user entries
	 * @param tomcat_file File object for tomcat-users.xml
	 */
	@Override
	public void load() {
		connect();
		refresh();
	}
	
	private void refresh() {
		user_list = readUserList();
		user_list_clone = cloneUserList();

	}
	
	private JsonObject connect(String urlstr) {
		try {
			URL url = new URL(urlstr);
			HttpURLConnection uc = (HttpURLConnection)url.openConnection();
			InputStream ucis = uc.getInputStream();
			JsonReader reader = Json.createReader(ucis);
			JsonObject jobj = reader.readObject();
			reader.close();
			return jobj;
		}
		catch (MalformedURLException mue) {
			LOGGER.severe("Unable to fetch "+urlstr+". "+mue);
		}
		catch (IOException ioe) {
			LOGGER.severe("Unable to connect to API. "+ioe);
		}
		return null;
	}
	/**
	 * Default connect. Must be refreshed after each operation
	 */
	private void connect() {
		json_conn = connect(aims_url);
	}
	

	
	/**
	 * Adds a user entry to the user_list and saves the result
	 * @param user Username
	 * @param email User email (can be consructed from username/org if null) 
	 */
	//public void addUser(String version, String userid, String username, String email, String requiresProgress, String organisation, String role) {
	public void addUser(String username, String email, String requiresProgress, String organisation, String role) {
		UserAIMS user = new UserAIMS();
		user.setUserName(username);
		user.setEmail(email);
		user.setRequiresProgress(requiresProgress);
		user.setOrganisation(organisation);
		user.setRole(AARoles.valueOf(role));
		addUser(user);
	}
	
	/**
	 * Allows email construction rather than assignment by passing null email arg
	 * @param username
	 */
	public void addUser(String username, String requiresProgress, String organisation, String role) {
		addUser(username, null, requiresProgress, organisation, role);
	}

	
	public void editUser(String ver, String uid, String uname, String email, String reqprg, String org, String role) {
		UserAIMS user = new UserAIMS();		
		user.setVersion(ver);
		user.setUserId(uid);
		user.setUserName(uname);
		user.setEmail(email);
		user.setRequiresProgress(reqprg);
		user.setOrganisation(org);
		user.setRole(AARoles.valueOf(role));
		//user_list.add(user);
		editUser(user);
	}
	
	public void editUser(String uname, String email, String reqprg, String org, String role) {
		UserAIMS user = new UserAIMS();
		user.setUserName(uname);
		user.setEmail(email);
		user.setRequiresProgress(reqprg);
		user.setOrganisation(org);
		user.setRole(AARoles.valueOf(role));
		//user_list.add(user);
		editUser(user);
	}
	
	/**
	 * Save changes to the user_list back to AIMS via the API
	 */
	@Override
	public void save() {

		try {
			for (ActionPayload ap : buildPayloadList()){
				URL url = new URL(aims_url+ap.plus);
				HttpURLConnection uc = (HttpURLConnection) url.openConnection();
				setParams(uc,ap.action.ppd);
				try (JsonWriter writer = Json.createWriter(uc.getOutputStream())) {
					writer.writeObject(ap.payload);
				}
				catch (Exception e) {
					LOGGER.warning("HTTP write failed with "+e);
				}
				int rc = uc.getResponseCode();
				if (rc<200 || rc>299) {
					LOGGER.warning("HTTP write failed with "+uc.getResponseMessage());
					//throw new Exception(uc.getResponseMessage().toString());
				}
			}
		}
		catch (IOException ioe) {
			System.err.println(ioe);
		}
		load();
	}

	private List<ActionPayload> buildPayloadList() {
		List<ActionPayload> ap_list = new ArrayList<>();
		//if active has X not in clone : add X
		for (User user : user_list) {
			if (!user_list_clone.contains(user)) {
				ap_list.add(buildPayload(user,Action.Add));
			}
		}
		//if clone has X not in active : del X
		for (User user_clone : user_list_clone) {
			if (!user_list.contains(user_clone)) {
				ap_list.add(buildPayload(user_clone,Action.Delete));
			}
		}
		//if active X haschanged from clone X : edit X
		for (User user_clone : user_list_clone) {
			for (User user : user_list) {
				if (((UserAIMS)user).hasChanged((UserAIMS)user_clone)) {
					ap_list.add(buildPayload(user,Action.Update));
				}
			}
		}
		return ap_list;
		
	}
	
	private ActionPayload buildPayload(User user, Action action) {
		String plus = "";
		JsonObject juser = null;
		switch(action) {
		case Add: 
			juser = Json.createObjectBuilder()
				.add("userName",user.getUserName())
				.add("email",((UserAIMS)user).getEmail())
				.add("requiresProgress",((UserAIMS)user).getRequiresProgress())
				.add("organisation",((UserAIMS)user).getOrganisation())
				.add("role",((UserAIMS)user).getRole().name())
				.build();
			break;
		case Delete: 
			juser = Json.createObjectBuilder()
				.add("version",((UserAIMS)user).getVersion())
				.add("userId",((UserAIMS)user).getUserId())
				.build();
			break;
		case Update: 
			juser = Json.createObjectBuilder()
				.add("version",((UserAIMS)user).getVersion())
				.add("userId",((UserAIMS)user).getUserId())
				.add("userName",user.getUserName())
				.add("email",((UserAIMS)user).getEmail())
				.add("requiresProgress",((UserAIMS)user).getRequiresProgress())
				.add("organisation",((UserAIMS)user).getOrganisation())
				.add("role",((UserAIMS)user).getRole().name())
				.build();
			break;
		}
		
		if (Action.Add!=action) { plus = "/"+String.valueOf(((UserAIMS)user).getUserId()); }
		LOGGER.fine("jpayload "+action.ppd+"-["+plus+"]-"+juser);
		return new ActionPayload(plus,action,juser);
	}
	private void setParams(HttpURLConnection uc,String ppd) {
		try {
			uc.setConnectTimeout(5000);
			uc.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			uc.setRequestProperty("Accept", "application/json");
			uc.setChunkedStreamingMode(0);
			uc.setDoOutput(true);
			uc.setDoInput(true);
			uc.setRequestMethod(ppd);
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	/**
	 * Rewrites the user_doc by deleting all existing users and replacing them with the users saved in the user_list
	 */
	@Override
	public void saveUserList() {
		//this isnt needed because modifications are saved with each call to add/del/upd
		save();
	}
	
	/**
	 * Parses AIMS json doc returning list of users
	 * @return List of users
	 */
	public List<User> readUserList(){
		List<User> user_list_new = new ArrayList<>();
		connect();
		JsonArray entities = json_conn.getJsonArray("entities");
		//get the entities hrefs to get userpages
		//LOGGER.info("J ents "+entities.size()); //sometimes the wrong number of entities are returned
		for (int i=0; i<entities.size(); i++) {
			//LOGGER.info("J get "+i);
			JsonObject jo1 = (JsonObject) entities.get(i);
			String href = jo1.getString("href");
			JsonObject c = connect(href);
			//because sometimes the entities list hasn't caught up with the latest deletes
			if (c!=null) {
				JsonObject userprops = c.getJsonObject("properties");
				UserAIMS user = new UserAIMS();
				user.setVersion(userprops.getInt("version"));
				user.setUserId(userprops.getInt("userId"));
				user.setUserName(userprops.getString("userName"));
				user.setEmail(userprops.containsKey("email")?userprops.getString("email"):null);
				user.setRequiresProgress(userprops.getBoolean("requiresProgress"));
				user.setOrganisation(userprops.getString("organisation"));
				user.setRole(userprops.getString("role"));
				user_list_new.add(user);
			}
			
		}
		return user_list_new;

	}

	/** Simple tostring */
	public String toString(){
		String users = "";
		for (User user : user_list) {
			users += user.getUserName()+",";
		}
		return "UserReader::"+aims_url+"\n"+users;
	}

	@Override
	public String encrypt(String pass) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<User> cloneUserList() {
		List<User> new_user_list = new ArrayList<>();
		for (User user : user_list) {
			new_user_list.add(new UserAIMS((UserAIMS) user));
		}
		return new_user_list;
	}

}
