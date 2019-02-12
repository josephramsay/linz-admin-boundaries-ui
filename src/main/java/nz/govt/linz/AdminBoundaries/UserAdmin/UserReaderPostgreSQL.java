package nz.govt.linz.AdminBoundaries.UserAdmin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.sql.DataSource;

import nz.govt.linz.AdminBoundaries.DABConnector;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserPostgreSQL.PGRoles;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserTomcat.TCRoles;

public class UserReaderPostgreSQL extends UserReader {
	
	private static final Logger LOGGER = Logger.getLogger(UserReaderPostgreSQL.class.getName());

	private DataSource datasource;
	private DABConnector dab_conn;

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
		dab_conn = null;//.close();
		
	}

	@Override
	public void load() {
		connect();
		refresh();
		
	}
	private void connect() {
		if (dab_conn == null) {
			dab_conn = new DABConnector(datasource);
		}
	}
	
	private void refresh() {
		user_list = readUserList();
		user_list_clone = cloneUserList();
	}

	/**
	 * Adds a user entry to the user_list and saves the result
	 * @param user Username
	 * @param pass Password unencrypted
	 */
	public void addUser(String uname, String roles) {
		UserPostgreSQL user = new UserPostgreSQL();
		user.setUserName(uname);
		user.setRoles(roles);
		/*
		 if (user_list.contains(user)) {	
			User existing = user_list.get(user_list.indexOf(user));
			((UserPostgreSQL)existing).merge((UserPostgreSQL)user); 
		}
		else { user_list.add(user); }
		*/
		addUser(user);
	}
	
	public void editUser(String uname, String roles) {
		//UserPostgreSQL user = (UserPostgreSQL) findInUserList(uname);		
		UserPostgreSQL new_user = new UserPostgreSQL();		
		new_user.setUserName(uname);
		new_user.setRoles(roles);
		//user_list.add(user);
		editUser(new_user);
	}
	
	
	/**
	 * Get the user_list and its memberships from pg_users filtering by the role 
	 * prefix (including aims) and username prefix (excluding aims)
	 */
	public List<User> readUserList() {
		String user_query = 
			"select usename,passwd,array_agg(rolname)\n" + 
			"from pg_user\n" + 
			"join pg_auth_members on pg_user.usesysid=pg_auth_members.member\n" + 
			"join pg_roles on (pg_roles.oid=pg_auth_members.roleid)\n" + 
			"where rolname like 'aims%'\n" + 
			"and usename not like 'aims%'" +
			"group by usename,passwd";
		List<User> new_user_list = new ArrayList<>();
		LOGGER.info("Connection "+dab_conn);
		connect();
		for (List<String> row : dab_conn.executeQuery(user_query,false)) {
			UserPostgreSQL user = new UserPostgreSQL();
			user.setUserName(row.get(0)); 
			user.setPassword(row.get(1)); 
			user.setRoles(row.get(2).substring(1,row.get(2).length()-1));
			//new_user_list.add(user);
			//merge roles

			if (new_user_list.contains(user)) {	
				User existing = new_user_list.get(new_user_list.indexOf(user));
				((UserPostgreSQL)existing).merge((UserPostgreSQL)user); 
			}
			else { new_user_list.add(user); }
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
					LOGGER.info("grant "+role);
					String query =	String.format("grant %s to %s", role.name(), user.getUserName());
					//System.out.println(query);
					dab_conn.executeQuery(query);
				}
			}
		}
		for (User user_clone : user_list_clone) {
			//IF user_clone not in user_list AND role is allowed THEN revoke
			if (!user_list.contains(user_clone)) {
				for (PGRoles role : ((UserPostgreSQL)user_clone).getRoles()) {
					LOGGER.info("revoke "+role);
					String query =	String.format("revoke %s from %s", role.name(), user_clone.getUserName());
					//System.out.println(query);
					dab_conn.executeQuery(query);
				}
			}
		}

		refresh();
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
