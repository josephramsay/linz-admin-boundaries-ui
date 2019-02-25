package nz.govt.linz.AdminBoundaries.UserAdmin;

import java.util.EnumSet;
import java.util.List;


public class UserTomcat extends User {
	public String password;
	public EnumSet<TCRoles> roles;
	
	public enum TCRoles { AIMS, manager_gui, manager_script, manager_jmx, manager_status, admin_gui, admin_script; 
		public String _name() {return name().replace("_","-"); }
		}
	
	public enum GSMethod { UserName, Password, Roles; }
	
	public UserTomcat(){ 
		setPassword("");
		setRoles(EnumSet.of(TCRoles.AIMS));
	}
	public UserTomcat(UserTomcat other){ 
		super(other);
		setPassword(other.getPassword());
		setRoles(other.getRoles());
	}

	public UserTomcat(String username,String password, String _roles){ 
		super(username);
		setPassword(password);
		roles = EnumSet.noneOf(TCRoles.class);
		setRoles(_roles);
	}

	/* encryption is done in reader */
	public void setPassword(String password) {this.password = password;}
	public String getPassword() {return this.password;}
	public void setRoleStr(String rolestr) {
		for (String role : rolestr.split(",")){
			roles.add(TCRoles.valueOf(role.replace("-","_")));
		}
	}
	public EnumSet<TCRoles> getRoles(){return roles;}
	public void setRoles(EnumSet<TCRoles> roles) { this.roles = roles;}
	public void setRoles(String roles) { setRoleStr(roles); }
	public void mergeRoles(EnumSet<TCRoles> roles) { this.roles.addAll(roles);}
	
	/**
	 * merge tomcat user can include role add/del and password changes
	 */
	@Override
	public void merge(User user) {
		//super.merge(user);
		//role add extra to set
		this.mergeRoles(((UserTomcat)user).getRoles());
		//change to new password
		this.setPassword(((UserTomcat)user).getPassword());
	}
	
	public String getRoleStr() {
		String rolestr = "";
		for (TCRoles role : roles) {
			rolestr += role._name()+",";
		}
		return rolestr.substring(0, rolestr.length() - 1);
	}
	
	//public List<String> getGSMethod() {return Stream.of(GSMethod.values()).map(Enum::name).collect(Collectors.toList()); }
	public List<String> getGSMethod() {return UserReader.getNames(GSMethod.class);}
	
	/**
	 * Compare function for two UserTC instances
	 */
	@Override
	public int compare(User user1, User user2) {
		int comp = super.compare(user1, user2);
		comp += user1 instanceof UserTomcat ? 0 : 1e10;
		comp += user2 instanceof UserTomcat ? 0 : 2e10;
		if ( comp != 0 ) return comp;
		comp += ((UserTomcat)user1).getRoles().equals( ((UserTomcat)user2).getRoles()) ? 0:1;
		return comp;
	}
	
	
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj)
			&& ( UserTomcat.class.isAssignableFrom(obj.getClass() ) );
			//&& ( this.userName.equals( ((UserTomcat)obj).userName) ); 
			//&& ( this.getRoles().equals( ((UserTomcat)obj).getRoles() ) );
	}
	/**
	 * default string rep
	 */
	public String toString() {
		return "UserTomcat:"+userName;
	}
}