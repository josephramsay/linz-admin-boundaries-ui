package nz.govt.linz.AdminBoundaries.UserAdmin;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class UserPostgreSQL extends User {
	public String password;
	public EnumSet<PGRoles> roles;
	
	enum PGRoles { aims_dba, aims_admin, aims_user, aims_reader; }
	
	public UserPostgreSQL(){ 
		setPassword("");
		setRoles(EnumSet.of(PGRoles.aims_reader));
	}
	public UserPostgreSQL(UserPostgreSQL other){ 
		super(other);
		setPassword(other.getPassword());
		setRoles(other.getRoles());
	}
	public void setPassword(String password) {this.password = password;}
	public String getPassword() {return this.password;}
	public void setRoles(EnumSet<PGRoles> roles) { this.roles = roles;}
	public EnumSet<PGRoles> getRoles() { return roles; }
	public void setRoleStr(String rolestr) {
		for (String role : rolestr.split(",")){
			roles.add(PGRoles.valueOf(role));
		}
	}
	
	@Override
	public void merge(User user) {
		//super.merge(user);
		//role add extra to set
		this.setRoles(((UserPostgreSQL)user).getRoles());
		//cant change password in PG because all we're doing re AIMS is adding/deleting fro AIMS groups
		//this.setPassword(((UserPostgreSQL)user).getPassword());
	}
	
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
	
	/**
	 * default string rep
	 */
	public String toString() {
		return "UserPostgreSQL:"+userName;
	}
}