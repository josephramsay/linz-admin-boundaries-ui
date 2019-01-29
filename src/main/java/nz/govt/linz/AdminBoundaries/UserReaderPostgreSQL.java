package nz.govt.linz.AdminBoundaries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;

import nz.govt.linz.AdminBoundaries.UserReaderAIMS.UserAIMS;

public class UserReaderPostgreSQL extends UserReader {
	
	public class UserPG extends User {
		public String password;
		public EnumSet<PGRoles> roles;
		
		public UserPG(){ 
			setPassword("");
			setRoles(EnumSet.of(PGRoles.aims_reader));
		}
		public UserPG(UserPG other){ 
			super(other);
			setPassword(other.getPassword());
			setRoles(other.getRoles());
		}
		public void setPassword(String password) {this.password = password;}
		public String getPassword() {return this.password;}
		public void setRoleStr(String rolestr) {
			for (String role : rolestr.split(",")){
				roles.add(PGRoles.valueOf(role));
			}
		}
		public void setRoles(EnumSet<PGRoles> roles) { this.roles = roles;}
		public EnumSet<PGRoles> getRoles() { return roles; }
		public String getRoleStr() {
			String rolestr = "";
			for (PGRoles role : roles) {
				rolestr += role+",";
			}
			return rolestr.substring(0, rolestr.length() - 1);
		}
		@Override
		public List<String> getSpringRolls() {
			List<String> springrolls = new ArrayList<>();
			for (PGRoles role : roles) {springrolls.add(role.name());}
			return springrolls;
		}
	}
	
	private DataSource datasource;
	private DABConnector conn;
	private enum PGRoles { aims_dba, aims_admin, aims_user, aims_reader; }
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
			User user = new UserPG();
			user.setUserName(row.get(0)); 
			((UserPG)user).setPassword(row.get(1)); 
			((UserPG)user).setRoleStr(row.get(2));
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
				for (PGRoles role : ((UserPG)user).getRoles()) {
					String query =	String.format("grant %s to %s", role.name(), user.getUserName());
					//System.out.println(query);
					conn.executeQuery(query);
				}
			}
		}
		for (User user_clone : user_list_clone) {
			//IF user_clone not in user_list AND role is allowed THEN revoke
			if (!user_list.contains(user_clone)) {
				for (PGRoles role : ((UserPG)user_clone).getRoles()) {
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
			new_user_list.add(new UserPG((UserPG)user));
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
