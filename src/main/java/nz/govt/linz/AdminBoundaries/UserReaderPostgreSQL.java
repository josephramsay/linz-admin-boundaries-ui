package nz.govt.linz.AdminBoundaries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

public class UserReaderPostgreSQL extends UserReader {
	
	private DataSource datasource;
	private DABConnector conn;
	
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
		user_list = readUserSection();
		
	}

	public List<Map<String, String>> readUserSection() {
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

	@Override
	public void addUser(String user, String pass, String role) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delUser(String user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void editUser(String user, String pass, String roles) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveUserSection() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * encrypt method to satisfy abstract requirement but also as a placeholder
	 */
	@Override
	public String encrypt(String pass) {
		return pass;
	}
}
