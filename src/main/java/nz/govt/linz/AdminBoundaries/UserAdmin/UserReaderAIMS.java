package nz.govt.linz.AdminBoundaries.UserAdmin;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerationException;

import nz.govt.linz.AdminBoundaries.DABServletUserAdmin;
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
	
	private static final String user_ref_base = "http://<SVR>:8080/aims/api/admin/users/";
	
	private String aims_url;
	private JsonObject aims_json_obj;
	private List<User> user_list_clone;
	
	/**
	 * Null constructor using default config file location 
	 */
	public UserReaderAIMS(){
		this(user_ref_base.replace("<SVR>",readCreds().get(1).split(":",2)[1]));
	}

	/**
	 * url constructor inits API access to aims user db
	 * @param procarg
	 */
	public UserReaderAIMS(String _aims_url){
		aims_url =  _aims_url;
		setDefAuth();
		load();
	}
	
	private void setDefAuth() {
		String[] u_p = readCreds().get(0).split(":",2);
		System.out.println(u_p[0]+"//"+u_p[1]);
		Authenticator.setDefault (new Authenticator() {
		    protected PasswordAuthentication getPasswordAuthentication() {
		        return new PasswordAuthentication (u_p[0], u_p[1].toCharArray());
		    }
		});
	}
	
	private static List<String> readCreds() { 
		List<String> lines = new ArrayList<String>();
		try {
			String line;
			BufferedReader reader = new BufferedReader(new FileReader("tempdat"));
			while ((line = reader.readLine()) != null) { lines.add(line);}
			reader.close();
		}
		catch (IOException ioe) {}
		return lines;//.get(0).split(":",2);
		
	}

	/**
	 * Load the tomcat-users file into local doc object and read a map of the user entries
	 * @param tomcat_file File object for tomcat-users.xml
	 */
	@Override
	public void load() {
		aims_json_obj = getJO(aims_url);
		user_list = readUserList();
		user_list_clone = cloneUserList();

	}
	private JsonObject getJO(String urlstr) {
		try {
			URL url = new URL(urlstr);
			HttpURLConnection uc = (HttpURLConnection)url.openConnection();
			InputStream ucis = uc.getInputStream();
			JsonReader reader = Json.createReader(ucis);
			JsonObject jobj = reader.readObject();
			reader.close();
			return jobj;
		}
		catch (IOException ioe) {
			System.err.println(ioe);
		}
		return null;
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
		if (!user_list.contains(user)) {
			user_list.add(user);
		}
		else {
			User orig = findInUserList(user.userName);
			((UserAIMS)orig).merge(user);
			user_list.remove(orig);
			user_list.add(orig);
		}
		saveUserList();
	}
	
	/**
	 * Allows email construction rather than assignment by passing null email arg
	 * @param username
	 */
	public void addUser(String username, String requiresProgress, String organisation, String role) {
		addUser(username, null, requiresProgress, organisation, role);
	}
	
	
	/**
	 * Shortcut to delete which finds existing user in list with highest ver 
	 * @uname User name
	 */
	public void delUser(String uname) {
		User user = findInUserList(uname);
		user_list.remove(user);
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
		//deluser...
		user_list.add(user);
	}
	
	
	/*
	addUser(uname,email,reqprg,org,role) X
	delUser(ver,uid)
	editUser(ver,uid,uname,email,reqprg,org,role)
	*/
	
	/**
	 * Save changes to the user_list back to AIMS via the API
	 */
	@Override
	public void save() {

		try {
			URL url = new URL(aims_url);
			HttpURLConnection uc = (HttpURLConnection) url.openConnection();
			setParams(uc);
			for (JsonObject payload : buildPayloads()){
				try (JsonWriter writer = Json.createWriter(uc.getOutputStream())) {
					writer.writeObject(payload);
				}
				int rc = uc.getResponseCode();
				System.out.println(rc);
			}
		}
		catch (IOException ioe) {
			System.err.println(ioe);
		}
		user_list = readUserList();
	}

	private List<JsonObject> buildPayloads() {
		List<JsonObject> pd_list = new ArrayList<>();
		//if active has X not in clone : add X
		for (User user : user_list) {
			System.out.println("--------------------\nTESTING :: "+user.userName);
			if (!user_list_clone.contains(user)) {
				pd_list.add(constructPayload(user,Action.Add));
			}
		}
		//if clone has X not in active : del X
		for (User user_clone : user_list_clone) {
			if (!user_list.contains(user_clone)) {
				pd_list.add(constructPayload(user_clone,Action.Delete));
			}
		}
		//if active X haschanged from clone X : edit X
		for (User user_clone : user_list_clone) {
			for (User user : user_list) {
				if (((UserAIMS)user).hasChanged((UserAIMS)user_clone)) {
					pd_list.add(constructPayload(user,Action.Update));
				}
			}
		}
		return pd_list;
		
	}
	
	private JsonObject constructPayload(User user, Action action) {
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
		System.out.println(juser);
		return juser;
	}
	private void setParams(HttpURLConnection uc) {
		try {
			uc.setConnectTimeout(5000);
			uc.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			uc.setRequestProperty("Accept", "application/json");
			uc.setChunkedStreamingMode(0);
			uc.setDoOutput(true);
			uc.setDoInput(true);
			uc.setRequestMethod("POST");
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	private void setCreds() {
		JsonObject credsx = Json.createObjectBuilder()
			.add("auth", Json.createObjectBuilder()
				.add("tenantName", "adm")
				.add("passwordCredentials", Json.createObjectBuilder()
					.add("username", "John")
					.add("password", "Smith")))
			.build();
		//return creds;
		JsonObject creds = Json.createObjectBuilder()
				.add("authentication", Json.createObjectBuilder()
						.add("username", "<u>")
						.add("password", "<p>"))
					.build();
		//return creds;
		/*
		String[] u_p = readCreds().get(0).split(":",2);
		System.out.println(u_p[0]+"//"+u_p[1]);
		Authenticator.setDefault (new Authenticator() { 
		    protected PasswordAuthentication getPasswordAuthentication() {
		        return new PasswordAuthentication (u_p[0], u_p[1].toCharArray());
		    }
		});
		*/
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
		JsonArray entities = aims_json_obj.getJsonArray("entities");
		//get the entities hrefs to get userpages
		for (int i=0; i<entities.size(); i++) {
			JsonObject jo1 = (JsonObject) entities.get(i);
			String href = jo1.getString("href");
			JsonObject userprops = getJO(href).getJsonObject("properties");
			
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

	@Override
	public void addUser(String username, String password, String roles) {
		LOGGER.warning("Require additional parameters to add AIMS user");
		
	}

}
