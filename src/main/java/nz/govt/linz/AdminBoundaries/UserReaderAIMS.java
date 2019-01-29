package nz.govt.linz.AdminBoundaries;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriter;

import nz.govt.linz.AdminBoundaries.User.Action;

//import org.apache.catalina.realm.UserDatabaseRealm;
//
//import nz.co.espatial.aims.db.DBUtils;
//import nz.co.espatial.aims.db.DBUtils.QueryTransaction;
//import nz.co.espatial.aims.db.model.User;
//import nz.co.espatial.aims.db.reference.UserDao;
//import nz.co.espatial.aims.AIMS;

public class UserReaderAIMS extends UserReader {
	
	private static final String user_ref_base = "http://tstassgeo02:8080/aims/api/admin/users/";
	
	public class UserAIMS extends User {
		public int version,userId;
		public String email;
		public boolean requiresProgress;
		public Organisation organisation;
		public EnumSet<AARoles> roles;
		
		public UserAIMS(){ 
			this.version = 0;
			this.userId = 0;
			this.userName = "";
			this.email = "";
			this.requiresProgress = false;
			this.organisation = Organisation.LINZ;
			this.roles = EnumSet.of(AARoles.Follower);
		}
		public UserAIMS(UserAIMS other){ 
			setVersion(other.getVersion());
			setUserId(other.getUserId());
			setUserName(other.getUserName());
			setRequiresProgress(other.getRequiresProgress());
			setOrganisation(other.getOrganisation());
			setRoles(other.getRoles());
			setEmail(other.getEmail());
		}
		
		public void setVersion(String version) { this.version = Integer.parseInt(version); }
		public String getVersion() { return String.valueOf(this.version); }
		public void setUserId(String userId) { this.userId = Integer.parseInt(userId); }
		public String getUserId() { return String.valueOf(this.userId); }
		public void setRequiresProgress(String requiresProgress) { this.requiresProgress = Boolean.valueOf(requiresProgress); }
		public String getRequiresProgress() { return String.valueOf(this.requiresProgress); }
		public void setOrganisation(String  organisation) { Organisation.valueOf(organisation); }
		public String getOrganisation() { return organisation.name(); }
		public void setEmail(String email) { 
			if (email != null) { this.email = email; }
			else if (this.userName != "") { this.email = this.userName+"@"+this.organisation.domain; }
		}
		public String getEmail() { return this.email; }
		public void setRoles(EnumSet<AARoles> roles) { this.roles = roles;}
		public EnumSet<AARoles> getRoles() { return roles; }
		//because AIMS only uses one role type add conv methods
		public void setRole(AARoles role) { this.roles = EnumSet.of(role);}
		public AARoles getRole() { return (AARoles) roles.toArray()[0]; }
		@Override
		public List<String> getSpringRolls() {
			List<String> springrolls = new ArrayList<>();
			for (AARoles role : roles) {springrolls.add(role.name());}
			return springrolls;
		}
	}
	
	private class UserAIMSComparator extends UserComparator{
		//@Override
		public int compare(UserAIMS user1, UserAIMS user2) {
			int comp = super.compare(user1, user2);
			comp += user1.version - user2.version;
			comp += user1.userId - user2.userId;
			comp += user1.userName.compareTo(user2.userName);
			comp += user1.getRole().ordinal() - user2.getRole().ordinal();
			comp += user1.organisation.ordinal() - user2.organisation.ordinal();
			comp += user1.email.compareTo(user2.email);
			return comp;
		}
		
	}
	
	private String aims_url;
	private JsonObject aims_jsonobj;
	private List<User> user_list, user_list_clone;
	
	private enum AARoles { Administrator,Publisher,Reviewer,Follower; }
	private enum Organisation { 
		LINZ("linz.govt.nz"), 
		eSpatial("e-spatial.co.nz"); 
		private final String domain;
		Organisation(String domain){ this.domain = domain;}
	}
	
	/**
	 * Null constructor using default config file location 
	 */
	public UserReaderAIMS(){
		this(user_ref_base);
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
		String[] u_p = readCreds();
		System.out.println(u_p[0]+"//"+u_p[1]);
		Authenticator.setDefault (new Authenticator() {Map<String, String> user;
		    protected PasswordAuthentication getPasswordAuthentication() {
		        return new PasswordAuthentication (u_p[0], u_p[1].toCharArray());
		    }
		});
	}
	
	private String[] readCreds() { 
		List<String> lines = new ArrayList<String>();
		try {
			String line;
			BufferedReader reader = new BufferedReader(new FileReader("tempcred"));
			while ((line = reader.readLine()) != null) { lines.add(line);}
			reader.close();
		}
		catch (IOException ioe) {}
	    return lines.get(0).split(":",2);
		
	}

	/**
	 * Load the tomcat-users file into local doc object and read a map of the user entries
	 * @param tomcat_file File object for tomcat-users.xml
	 */
	@Override
	public void load() {

		try {
			URL url = new URL(aims_url);
			HttpURLConnection uc = (HttpURLConnection)url.openConnection();
			InputStream ucis = uc.getInputStream();
			JsonReader reader = Json.createReader(ucis);
			aims_jsonobj = reader.readObject();//.readArray();
			reader.close();
		}
		catch (IOException ioe) {
			System.err.println(ioe);
		}
		user_list = readUserList();
		user_list_clone = cloneUserList();

	}

	
	/**
	 * Adds a user entry to the user_list and saves the result
	 * @param user Username
	 * @param pass Password unencrypted
	 */
	public void addUser(String username, String email, String requiresProgress, String organisation, String role) {
		User user = new UserAIMS();
		user.setUserName(username);
		((UserAIMS)user).setEmail(email);
		((UserAIMS)user).setRequiresProgress(requiresProgress);
		((UserAIMS)user).setOrganisation(organisation);
		((UserAIMS)user).setRole(AARoles.valueOf(role));
		user_list.add(user);
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
	 * Save changes to the user_list back to AIMS via the API
	 */
	@Override
	public void save() {
		
		try {
			URL url = new URL(aims_url);
			HttpURLConnection uc = (HttpURLConnection)url.openConnection();
			OutputStream ucos = uc.getOutputStream();
			JsonWriter writer = Json.createWriter(ucos);
			
			//if active has Y not in clone add Y
			for (User user : user_list) {
				if (!user_list_clone.contains(user)) {
					JsonObject juser = constructPayload(user,Action.Add);
					writer.write(juser);
				}
			}
			//if clone has X not in active del X
			for (User user_clone : user_list_clone) {
				if (!user_list.contains(user_clone)) {
					JsonObject juser = constructPayload(user_clone,Action.Delete);
					writer.write(juser);
				}
			}
			//if clone X.username != active X.username edit X
			for (User user_clone : user_list_clone) {
				for (User user : user_list) {
					if (((UserAIMS)user).compare((UserAIMS)user_clone) == 0) {
						JsonObject juser = constructPayload(user,Action.Update);
						writer.write(juser);
					}
				}
			}
			writer.close();
		}
		catch (IOException ioe) {
			System.err.println(ioe);
		}
		user_list = readUserList();
	}

	private JsonObject constructPayload(User user, Action action) {
		JsonObject juser = null;
		switch(action) {
		case Add: juser = Json.createObjectBuilder()
				.add("userName",user.getUserName())
				.add("email",((UserAIMS)user).getEmail())
				.add("requiresProgress",((UserAIMS)user).getRequiresProgress())
				.add("organisation",((UserAIMS)user).getOrganisation())
				.add("role",((UserAIMS)user).getRole().name())
				.build();
		case Delete: juser = Json.createObjectBuilder()
				.add("version",((UserAIMS)user).getVersion())
				.add("userId",((UserAIMS)user).getUserId())
				.build();
		case Update: juser = Json.createObjectBuilder()
				.add("version",((UserAIMS)user).getVersion())
				.add("userId",((UserAIMS)user).getUserId())
				.add("userName",user.getUserName())
				.add("email",((UserAIMS)user).getEmail())
				.add("requiresProgress",((UserAIMS)user).getRequiresProgress())
				.add("organisation",((UserAIMS)user).getOrganisation())
				.add("role",((UserAIMS)user).getRole().name())
				.build();
		}
		return juser;
	}
	
	
	/**
	 * Rewrites the user_doc by deleting all existing users and replacing them with the users saved in the user_list
	 */
	@Override
	public void saveUserList() {
		//this isnt needed because modifications are saved with each call to add/del/upd
	}
	
	/**
	 * Parses tomcat users file returning map of user/pass entries
	 * @param doc Document object of tomcat-users file
	 * @return HashMap of user/password pairs
	 */
	public List<User> readUserList(){
		List<User> user_list_new = new ArrayList<>();
		JsonArray ja = aims_jsonobj.getJsonArray("properties");
		for (int i=0; i<ja.size(); i++) {
			User user = new UserAIMS();
			JsonObject jo = (JsonObject) ja.get(i);
			((UserAIMS)user).setVersion(jo.getString("version"));
			((UserAIMS)user).setUserId(jo.getString("userId"));
			user.setUserName(jo.getString("userName"));
			((UserAIMS)user).setRequiresProgress(jo.getString("requiresProgress"));
			((UserAIMS)user).setOrganisation(jo.getString("organisation"));
			((UserAIMS)user).setRole(AARoles.valueOf(jo.getString("role")));
			//String href =  jo.getString("href");
			//user.setUserId(href.substring(href.lastIndexOf("/")+1));
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
			new_user_list.add(new UserAIMS((UserAIMS)user));
		}
		return new_user_list;
	}

}
