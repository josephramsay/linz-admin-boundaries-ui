package nz.govt.linz.AdminBoundaries.UserAdmin;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wrapper class for user map
 * @author jramsay
 *
 */
public abstract class User implements Comparator<User> {
	
	private static final Logger LOGGER = Logger.getLogger(User.class.getName());
	
	public enum GSMethod { Version, UserId, UserName, Email, RequiresProgress, Organisation, Role, Password; 
		public String lc() {return name().toLowerCase();}
	}
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
	
	/**
	 * Reflective getter/setter method caller
	 * @param gsname
	 * @param gsop
	 * @param param
	 * @return
	 */
	private Object callMethod(GSMethod gsname, GSOp gsop, String param){
		try {
			Method method;
			if (gsop == GSOp.get) {
				method = this.getClass().getMethod("get"+gsname.name());
			}
			else {
				method = this.getClass().getMethod("set"+gsname.name(), String.class);
			}
			
			return method.invoke(this);
		} 
		catch (SecurityException se) { 
			LOGGER.warning("Error calling geter method" + se);
		}
		catch (NoSuchMethodException nsme) { 
			LOGGER.warning("Requested getter method doesn't exist. " + nsme); 
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
	public Object getUserMethod(GSMethod gsname){
		return callMethod(gsname, GSOp.get, null);
	}
	
	/**
	 * Call the setter for this field ref using a string arg
	 * @param gsname
	 * @param param
	 * @return
	 */
	public Object setUserMethod(GSMethod gsname,String param){
		return callMethod(gsname, GSOp.set, param);
	}
	
	/**
	 * Given the name of a field find a matching method ref
	 * @param name
	 * @return
	 */
	public GSMethod matchFieldName(String name) {
		for (GSMethod gsm : GSMethod.values()) {
			if (name.equals(gsm.lc())){return gsm;}
		}
		return null;
		
	}
	
	/**
	 * Find all the getter/setter methods for this user type
	 * @param user
	 * @return Map of getters/setters against their respective names
	 */
	public static Map<String,Method> getReadMethods(User user) {
		Map<String,Method> glist = new HashMap<>();
		Class<? extends User> objClass= user.getClass();

		Method[] methods = objClass.getMethods();
		for (Method method : methods) {
			if ("et".equals(method.getName().substring(1,3))) {
				glist.put(method.getName().substring(3),method);
			}
		}
		return glist;
	}
	
	/**
	 * default string rep
	 */
	public String toString() {
		return "User:"+userName;
	}

}