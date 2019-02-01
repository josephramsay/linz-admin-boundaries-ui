package nz.govt.linz.AdminBoundaries.UserAdmin;

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import nz.govt.linz.AdminBoundaries.DABConnector;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserPostgreSQL.PGRoles;

public class UserReaderPostgreSQL extends UserReader {
	
	private DataSource datasource;
	private DABConnector conn;

	private List<User> user_list_clone;
	
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
		conn = null;//.close();
		
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
	 * Adds a user entry to the user_list and saves the result
	 * @param user Username
	 * @param pass Password unencrypted
	 */
	public void addUser(String username, String password, String roles) {
		User user = new UserPostgreSQL();
		user.setUserName(username);
		((UserPostgreSQL)user).setPassword(password);
		((UserPostgreSQL)user).setRoleStr(roles);
		user_list.add(user);
		saveUserList();
	}
	
	/**
	 * Get the user_list and its memberships from pg_users filtering by the role 
	 * prefix (including aims) and username prefix (excluding aims)
	 */
	public List<User> readUserList() {
		String user_query = 
			"select usename,passwd,rolname\n" + 
			"from pg_user\n" + 
			"join pg_auth_members on pg_user.usesysid=pg_auth_members.member\n" + 
			"join pg_roles on (pg_roles.oid=pg_auth_members.roleid)\n" + 
			"where rolname like 'aims%'\n" + 
			"and usename not like 'aims%'";
		List<User> new_user_list = new ArrayList<>();
		for (List<String> row : conn.executeQuery(user_query,false)) {
			User user = new UserPostgreSQL();
			user.setUserName(row.get(0)); 
			((UserPostgreSQL)user).setPassword(row.get(1)); 
			((UserPostgreSQL)user).setRoleStr(row.get(2));
			new_user_list.add(user);
		}
		return new_user_list;
		
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
		for (User user : user_list) {
			//IF user not in usr_list_clone AND role is allowed THEN grant
			if (!user_list_clone.contains(user)){
				for (PGRoles role : ((UserPostgreSQL)user).getRoles()) {
					String query =	String.format("grant %s to %s", role.name(), user.getUserName());
					//System.out.println(query);
					conn.executeQuery(query);
				}
			}
		}
		for (User user_clone : user_list_clone) {
			//IF user_clone not in user_list AND role is allowed THEN revoke
			if (!user_list.contains(user_clone)) {
				for (PGRoles role : ((UserPostgreSQL)user_clone).getRoles()) {
					String query =	String.format("revoke %s from %s", role.name(), user_clone.getUserName());
					//System.out.println(query);
					conn.executeQuery(query);
				}
			}
		}
		user_list_clone = cloneUserList();
	}

	@Override
	public List<User> cloneUserList() {
		List<User> new_user_list = new ArrayList<>();
		for (User user : user_list) {
			new_user_list.add(new UserPostgreSQL((UserPostgreSQL)user));
		}
		return new_user_list;
	}
	
	/**
	 * encrypt method to satisfy abstract requirement but also as a placeholder
	 */
	@Override
	public String encrypt(String pass) {
		return pass;
	}

}
