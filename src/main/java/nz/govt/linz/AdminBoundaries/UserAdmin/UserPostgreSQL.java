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
		//can't change password in PG because all we're doing re AIMS is adding/deleting from AIMS groups
		//this.setPassword(((UserPostgreSQL)user).getPassword());
	}
	
	/**
	 * Compare function for two UserPG instances
	 */
	@Override
	public int compare(User user1, User user2) {
		int comp = super.compare(user1, user2);
		comp += user1 instanceof UserPostgreSQL ? 0 : 1e10;
		comp += user2 instanceof UserPostgreSQL ? 0 : 2e10;
		if ( comp != 0 ) return comp;
		comp += ((UserPostgreSQL)user1).getRoles().equals( ((UserPostgreSQL)user2).getRoles()) ? 0:1;
		return comp;
	}
	
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj)
			&& ( UserPostgreSQL.class.isAssignableFrom(obj.getClass() ) );
			//&& ( this.userName.equals( ((UserPostgreSQL)obj).userName) ) 
			//&& ( this.getRoles().equals( ((UserPostgreSQL)obj).getRoles() ) );
	}
	
	
	/**
	 * Decides whether we need to update a user entry based on the user id being the same 
	 * but any of the other fields (ignoring version) having differences
	 * @return
	 */
	public boolean hasChanged(UserPostgreSQL other) {
		if (this.userName.equals(other.userName) && 
			!this.getRoles().equals(other.getRoles())
			) {
			return true;
		}
		return false;
		
	}
	
	/**
	 * default string rep
	 */
	public String toString() {
		return "UserPostgreSQL:"+userName;
	}
}