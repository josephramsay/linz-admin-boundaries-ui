package nz.govt.linz.AdminBoundaries.UserAdmin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public class UserAIMS extends User {
	public int version,userId;
	public String email;
	public boolean requiresProgress;
	public Organisation organisation;
	public EnumSet<AARoles> roles;
	
	enum AARoles { Administrator,Publisher,Reviewer,Follower; }
	
	enum Organisation { 
		LINZ("linz.govt.nz"), 
		e_Spatial("e-spatial.co.nz"); 
		private final String domain;
		Organisation(String domain){ this.domain = domain;}
		private String _name() {return name().replace("_","-");}
	}
	
	public UserAIMS(String userName) {
		this();
		this.userName = userName;
	}
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
	public void setVersion(int version) { this.version = version; }
	public int getVersion() { return this.version; }
	public void setUserId(String userId) { this.userId = Integer.parseInt(userId); }
	public void setUserId(int userId) { this.userId = userId; }
	public int getUserId() { return this.userId; }
	public void setRequiresProgress(String requiresProgress) { this.requiresProgress = Boolean.valueOf(requiresProgress); }
	public void setRequiresProgress(boolean requiresProgress) { this.requiresProgress = requiresProgress; }
	public boolean getRequiresProgress() { return this.requiresProgress; }
	public void setOrganisation(String  organisation) { this.organisation = Organisation.valueOf(organisation.replace("-","_")); }
	public String getOrganisation() { return organisation._name(); }
	public void setEmail(String email) { this.email = email != null && email != "" ? email : constructEmail(); }
	public String getEmail() { return this.email != null && this.email != "" ? this.email : constructEmail(); }
	public void setRoles(EnumSet<AARoles> roles) { this.roles = roles;}
	public EnumSet<AARoles> getRoles() { return roles; }
	//because AIMS only uses one role type add conv methods
	public void setRole(AARoles role) { this.roles = EnumSet.of(role);}
	public void setRole(String role) { this.roles = EnumSet.of(AARoles.valueOf(role));}
	public AARoles getRole() { return (AARoles) roles.toArray()[0]; }
	@Override
	public List<String> getSpringRolls() {
		List<String> springrolls = new ArrayList<>();
		for (AARoles role : roles) {springrolls.add(role.name());}
		return springrolls;
	}
	
	/**
	 * The rules for merge are;
	 * dont bother with version and userid, they're set in the api 
	 * username must be the same
	 * orig<-copy if orig is null
	 * change org and email
	 * add role to enumset<role>. for useraims which has only one role change it
	 * @param user
	 */
	public void merge(UserAIMS user) {
		//roll add extra to set
		this.setRole(user.getRole());
		//change to new org
		this.setOrganisation(user.getOrganisation());
		//change to new email
		this.setEmail(user.getEmail());
	}
	
	private String constructEmail() {
		return (this.userName != "" && this.organisation.domain != "") ? this.userName+"+CONS@"+this.organisation.domain : ""; 
	}
	@Override
	public int compare(User user1, User user2) {
		int comp = super.compare(user1, user2);
		comp += user1 instanceof UserAIMS ? 0 : 1e10;
		comp += user2 instanceof UserAIMS ? 0 : 2e10;
		if (comp!=0) return comp;
		comp += ((UserAIMS)user1).version - ((UserAIMS)user2).version;
		comp += ((UserAIMS)user1).userId - ((UserAIMS)user2).userId;
		comp += ((UserAIMS)user1).userName.compareTo(((UserAIMS)user2).userName);
		comp += ((UserAIMS)user1).getRole().ordinal() - ((UserAIMS)user2).getRole().ordinal();
		comp += ((UserAIMS)user1).organisation.ordinal() - ((UserAIMS)user2).organisation.ordinal();
		comp += ((UserAIMS)user1).email.compareTo(((UserAIMS)user2).email);
		return comp;
	}
	
	public int compareTo(UserAIMS user) {
		return compare(this,user);
	}	
	
	/**
	 * Minimal compare intended for list.contains(obj) context 
	 */
	@Override 
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (!User.class.isAssignableFrom(obj.getClass())) { return false; }
		//if (super.compareTo((User)obj) != 0 ) { return false; }
		//Minimal check of username 
		if (!this.userName.equals( ((UserAIMS)obj).userName )) { 
			System.out.println("NOT EQUAL :: "+this.userName+"/"+((UserAIMS)obj).userName);
			return false; 
		}
		//Comprehensive equals() using compareTo() to compare each object field
		//if (this.compareTo((UserAIMS) obj) != 0 ) { return false; }
		System.out.println("EQUAL :: "+this.userName+"/"+((UserAIMS)obj).userName);
		return true;
	}	
	
	@Override
	public int hashCode() {
		int hash = 3;
	
		hash = 53 * hash + (this.version != 0 ? this.version : 0);
		hash = 53 * hash + (this.userId != 0 ? this.userId : 0);
		hash = 53 * hash + (this.userName!= null ? this.userName.hashCode() : 0);
		hash = 53 * hash + (this.getRole() != null ? this.getRole().hashCode() : 0);
		hash = 53 * hash + (this.organisation != null ? this.organisation.hashCode() : 0);
		hash = 53 * hash + (this.email!= null ? this.email.hashCode() : 0);
		return hash;
	}
	
	/**
	 * Decides whether we need to update a user entry based on he user id being the same 
	 * but any of the other fields (ignoring version) having differences
	 * @return
	 */
	public boolean hasChanged(UserAIMS other) {
		if (this.userId == other.userId && 
			(!this.userName.equals(other.userName) || 
			 !this.getRole().equals(other.getRole()) ||
			 !this.organisation.equals(other.organisation) ||
			 !this.email.equals(other.email))
			) {
			return true;
		}
		return false;
		
	}
}