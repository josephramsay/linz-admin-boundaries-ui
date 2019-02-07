package nz.govt.linz.AdminBoundaries.UserAdmin;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import nz.govt.linz.AdminBoundaries.UserAdmin.UserAIMS.Organisation;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wrapper class for user map
 * @author jramsay
 *
 */
public abstract class User implements Comparator<User> {
	
	private static final Logger LOGGER = Logger.getLogger(User.class.getName());
	
//	public enum GSMethod { Version, UserId, UserName, Email, RequiresProgress, Organisation, Role, Roles, RoleStr, Password;		
//		public String lc() {return name().toLowerCase();}
//		/**
//		 * Given a string 
//		 * @param gsm
//		 * @return
//		 */
//		static GSMethod translate(String gsm){
//		    switch (gsm) {
//		    case "version": return Version;
//		    case "userid": return UserId;
//		    case "username": return UserName;
//		    case "email": return Email;
//		    case "requiresprogress": return RequiresProgress;
//		    case "organisation": return Organisation;
//		    case "role": return Role;
//		    case "roles": return Roles;
//		    case "rolestr": return RoleStr;
//		    case "password": return Password;
//		    default: throw new IllegalArgumentException(String.valueOf(gsm));
//		    }
//		}
//	}
	
	public enum GSOp { get, set; }
	public enum Action { Add("POST"),Delete("DELETE"),Update("PUT"); 
		String ppd;
		Action(String ppd){this.ppd = ppd;}
	}
	
	public String userName;
	
	/**
	 * Default user constructor
	 */
	public User(String userName){ 
		setUserName(userName);
	}
	public User() {
		this("");
	}
	public User(User user) {
		//setUserName(user.getUserName());
		this(user.getUserName());
	} 

	/** getters/setters */
	public void setUserName(String  userName) { this.userName = userName; }
	public String getUserName() { return this.userName; }
	public abstract List<String> getSpringRolls();
	
	@Override
	public int compare(User user1, User user2) {
		return user1.userName.compareTo(user2.userName);
	}
	
	public int compareTo(User user) {
		return compare(this,user);
	}

	@Override 
	public boolean equals(Object obj) {
		if (obj == null) { return false; }
		if (!User.class.isAssignableFrom(obj.getClass())) { return false; }
	
		if ((this.userName == null) ? (((User) obj).userName != null) : this.compareTo((User) obj) != 0 ) { return false; }
		
		return true;
	}
	
	/** Merge user_new into user_old by replacing where changed */
	public void merge(User user) {
		//This only really applies in the AIMS case
		this.setUserName(((UserAIMS)user).getUserName());
	}
	
	@Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.userName != null ? this.userName.hashCode() : 0);
        return hash;
    }
	
	
	Object invokeMethod(Method method) {
		try {
			return method.invoke(this);
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * Reflective getter/setter method caller
	 * @param gsname
	 * @param gsop
	 * @param param
	 * @return
	 */
	private Object callMethod(String gsname, GSOp gsop, String param){
		try {
			if (gsop == GSOp.get) {
				Method method = this.getClass().getMethod("get"+gsname);
				return method.invoke(this);
			}
			else {
				Method method = this.getClass().getMethod("set"+gsname, String.class);
				return method.invoke(this,param);
			}
		} 
		catch (SecurityException se) { 
			LOGGER.warning("Error calling "+gsop.name()+"ter method" + se);
		}
		catch (NoSuchMethodException nsme) { 
			LOGGER.warning("Requested "+gsop.name()+"ter method doesn't exist. " + nsme); 
		} 
		catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Call the getter for this field ref
	 * @param gsname
	 * @return
	 */
	public Object userGetterMethod(String gsname){
		return callMethod(gsname, GSOp.get, null);
	}
	
	/**
	 * Call the setter for this field ref using a string arg
	 * @param gsname
	 * @param param
	 * @return
	 */
	public Object userSetterMethod(String gsname, String param){
		return callMethod(gsname, GSOp.set, param);
	}
	
	
	/**
	 * Find all the getter/setter methods for this user type
	 * @param user
	 * @return Map of getters/setters against their respective names
	 */
	private static Map<String,Method> userAccessorMethods(User user,String prefix) {
		Map<String,Method> glist = new HashMap<>();
		Class<? extends User> objClass = user.getClass();

		Method[] methods = objClass.getMethods();
		for (Method method : methods) {
			if (prefix.equals(method.getName().substring(0,3))) {
				glist.put(method.getName().substring(3),method);
			}
		}
		return glist;
	}
	public static Map<String,Method> readMethods(User user) {
		return userAccessorMethods(user,"get");
	}
	public static Map<String,Method> writeMethods(User user) {
		return userAccessorMethods(user,"set");
	}
	
	/**
	 * default string rep
	 */
	public String toString() {
		return "User:"+userName;
	}

}