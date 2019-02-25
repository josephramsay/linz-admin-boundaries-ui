package nz.govt.linz.AdminBoundaries.UserAdmin;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Base user class representing an AIMS user in any of the tomcat/postgres/aims types.
 * The only common attribute across the three classes is username but this must be sync'd
 * for the successful use of AIMS
 * @author jramsay
 *
 */
public abstract class User implements Comparator<User> {
	
	private static final Logger LOGGER = Logger.getLogger(User.class.getName());
	
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
	/**
	 * Copy constructor (base)
	 * @param user
	 */
	public User(User user) {
		//setUserName(user.getUserName());
		this(user.getUserName());
	} 

	/** getters/setters */
	public void setUserName(String  userName) { this.userName = userName; }
	public String getUserName() { return this.userName; }
	
	/** Abstract for the GSmethod which returns field names */
	public abstract List<String> getGSMethod();
	//public List<String> getGSMethod() {return Stream.of(GSMethod.values().toString()).collect(Collectors.toList()); }
	
	@Override
	public int compare(User user1, User user2) {
		return user1.userName.compareTo(user2.userName);
	}
	
	public int compareTo(User user) {
		return compare(this,user);
	}

	/**
	 * Basic equals override tests username but calls the respective compareTo function s only
	 */
	@Override 
	public boolean equals(Object obj) {
		return ( obj != null )
			&& ( User.class.isAssignableFrom(obj.getClass()) )
			&& ( this.userName.equals(((User)obj).getUserName()) );
		/* Because this uses the subclass compareTo we get a full attribute 
		   equals() which is probably not what we want */
			//&& ( this.compareTo((User)obj) == 0 );
	}
	
	/** Merge user_new into user_old by replacing where changed */
	public void merge(User user) {
		LOGGER.info("user merge");
		//This only really applies in the AIMS case
		this.setUserName(((UserAIMS)user).getUserName());
	}
	
	/**
	 * Basic hashcode generator using username
	 */
	@Override
    public int hashCode() {
        int hash = 3;
        hash = 53 * hash + (this.userName != null ? this.userName.hashCode() : 0);
        return hash;
    }
	
	/**
	 * Shortcut for method.invoke
	 * @param method
	 * @return
	 */
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
	 * Call the getter for the named field
	 * @param gsname
	 * @return
	 */
	public Object readUserAttribute(String gsname){
		return callMethod(gsname, GSOp.get, null);
	}
	
	/**
	 * Call the setter for the named field using a string arg
	 * @param gsname Attribute name
	 * @param param String value of the attribute
	 * @return
	 */
	public Object writeUserAttribute(String gsname, String param){
		return callMethod(gsname, GSOp.set, param);
	}
	
	
	/**
	 * Find all the getter/setter methods for this user type
	 * @param user
	 * @return Map of getters/setters against their respective names
	 */
	private static Map<String,Method> userAccessorMethods(User user,GSOp gsop) {
		Map<String,Method> glist = new HashMap<>();

		for (Method method : user.getClass().getMethods()) {
			if (gsop.name().equals(method.getName().substring(0,3))) {
				glist.put(method.getName().substring(3),method);
			}
		}
		return glist;
	}
	
	/** Shortcut for the getter methods */
	public static Map<String,Method> readMethods(User user) {
		return userAccessorMethods(user,GSOp.get);
	}
	
	/** Shortcut for the setter methods */
	public static Map<String,Method> writeMethods(User user) {
		return userAccessorMethods(user,GSOp.set);
	}
	
	/**
	 * default string rep
	 */
	public String toString() {
		return "User:"+userName;
	}

}