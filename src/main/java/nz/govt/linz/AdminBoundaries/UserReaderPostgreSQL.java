package nz.govt.linz.AdminBoundaries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

public class UserReaderPostgreSQL extends UserReader {
	
	private DataSource datasource;
	private DABConnector conn;
	private static List<String> allowed_roles = 
		Arrays.asList("aims_dba","aims_admin","aims_user","aims_reader");
	private List<Map<String,String>> user_list_clone;
	
	/**
	 * Null constructor using default config file location 
	 */
	public UserReaderPostgreSQL(){
		load();
	}

	/**
	 * Constructor sets up config file path and tests accessibility
	 * @param procarg
	 */
	public UserReaderPostgreSQL(DataSource _datasource){
		datasource = _datasource;
		load();
	}

	@Override
	public void save() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void load() {
		if (datasource != null ) {
			conn = new DABConnector(datasource);
		}
		else {
			conn = new DABConnector();
		}
		user_list = readUserList();
		user_list_clone = cloneUserList();
		
	}

	/**
	 * Low level copy of user_list structure
	 * @return Copy of the user list
	 */
	private List<Map<String,String>> cloneUserList(){
		List<Map<String,String>> new_user_list = new ArrayList<>();
		for (Map<String,String> user_entry : user_list) {
			Map<String,String> new_user_entry = new HashMap<>();
			for (String key : user_entry.keySet()) {
				new_user_entry.put(key, user_entry.get(key));
			}
			new_user_list.add(new_user_entry);
		}
		return new_user_list;
	}
	
	/**
	 * Get the user_list and its memberships from pg_users filtering by the role 
	 * prefix (including aims) and username prefix (excluding aims)
	 */
	public List<Map<String, String>> readUserList() {
		String user_query = 
			"select usename,passwd,rolname\n" + 
			"from pg_user\n" + 
			"join pg_auth_members on pg_user.usesysid=pg_auth_members.member\n" + 
			"join pg_roles on (pg_roles.oid=pg_auth_members.roleid)\n" + 
			"where rolname like 'aims%'\n" + 
			"and usename not like 'aims%'";
		user_list = new ArrayList<Map<String, String>>();
		for (List<String> row : conn.executeQuery(user_query,false)) {
			Map<String,String> u_r = new HashMap<String,String>();
			u_r.put("username", row.get(0)); 
			u_r.put("password", row.get(1)); //This wont be anything, ***
			u_r.put("roles", row.get(2));
			user_list.add(u_r);
			
		}
		return user_list;
		
	}

	/**
	 * Writes back the user_list to the database by setting the appropriate grants 
	 * and making sure only one role per user since permissions are supposed to be 
	 * overlapping where needed.
	 * We don't add/delete users because they will be existing users that just want 
	 * (don't want) aims access
	 */
	@Override
	public void saveUserList() {
		for (Map<String,String> user : user_list) {
			//IF user not in usr_list_clone AND role is allowed THEN grant
			if (!user_list_clone.contains(user) &&
					allowed_roles.contains(user.get("roles"))){
				String query =	String.format("grant %s to %s", user.get("roles"), user.get("username"));
				//System.out.println(query);
				conn.executeQuery(query);
			}
		}
		for (Map<String,String> user_clone : user_list_clone) {
			//IF user_clone not in user_list AND role is allowed THEN revoke
			if (!user_list.contains(user_clone) && 
				allowed_roles.contains(user_clone.get("roles"))){
				String query =	String.format("revoke %s from %s", user_clone.get("roles"), user_clone.get("username"));
				//System.out.println(query);
				conn.executeQuery(query);
			}
		}
		user_list_clone = cloneUserList();
	}

	/**
	 * encrypt method to satisfy abstract requirement but also as a placeholder
	 */
	@Override
	public String encrypt(String pass) {
		return pass;
	}
}
