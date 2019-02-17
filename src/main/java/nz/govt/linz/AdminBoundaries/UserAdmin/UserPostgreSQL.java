package nz.govt.linz.AdminBoundaries.UserAdmin;

import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;

public class UserPostgreSQL extends User {
	
	private static final Logger LOGGER = Logger.getLogger(UserPostgreSQL.class.getName());
	
	public String password;
	public EnumSet<PGRoles> roles;
	
	public enum PGRoles { aims_admin, aims_user, aims_reader; }
	
	public enum GSMethod { UserName, Password, Roles; }
	
	public UserPostgreSQL(){ 
		setPassword("");
		setRoles(EnumSet.noneOf(PGRoles.class));
	}
	public UserPostgreSQL(UserPostgreSQL other){ 
		super(other);
		setPassword(other.getPassword());
		setRoles(other.getRoles());
	}
	public UserPostgreSQL(String username,String password, String _roles){ 
		super(username);
		setPassword(password);
		roles = EnumSet.noneOf(PGRoles.class);
		setRoles(_roles);
	}
	
	public void setPassword(String password) {this.password = password;}
	public String getPassword() {return this.password;}
	public void setRoles(EnumSet<PGRoles> roles) { this.roles = roles; }
	public void setRoles(String roles) { setRoleStr(roles); }
	public void setRole(PGRoles role) { this.roles.add(role); }
	public void mergeRoles(EnumSet<PGRoles> roles) { this.roles.addAll(roles); }
	public EnumSet<PGRoles> getRoles() { return roles; }
	public void setRoleStr(String rolestr) {
		for (String role : rolestr.split(",")){
			roles.add(PGRoles.valueOf(role));
		}
	}
	
	public String getRoleStr() {
		String rolestr = "";
		for (PGRoles role : roles) {
			rolestr += role+",";
		}
		return rolestr.substring(0, rolestr.length() - 1);
	}
	
	//public List<String> getGSMethod() {return Stream.of(GSMethod.values()).map(Enum::name).collect(Collectors.toList()); }
	public List<String> getGSMethod() {return UserReader.getNames(GSMethod.class);}
	
	@Override
	public void merge(User user) {
		//super.merge(user);
		//role add extra to set
		LOGGER.info("add roles: "+((UserPostgreSQL)user).getRoles());
		this.setRoles(((UserPostgreSQL)user).getRoles());
		//cant change password in PG because all we're doing re AIMS is adding/deleting fro AIMS groups
		//this.setPassword(((UserPostgreSQL)user).getPassword());
	}
	
	
	
	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) { return false; }
		final UserPostgreSQL that = (UserPostgreSQL) obj;
		
		if ( this.getRoles() != that.getRoles() ) { return false; }
		
		return true;
	}
	
	
	/**
	 * default string rep
	 */
	public String toString() {
		return "UserPostgreSQL:"+userName;
	}
}