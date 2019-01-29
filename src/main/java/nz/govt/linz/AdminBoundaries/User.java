package nz.govt.linz.AdminBoundaries;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import nz.govt.linz.AdminBoundaries.UserReaderAIMS.UserAIMS;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Wrapper class for user map
 * @author jramsay
 *
 */
public abstract class User {
	
	private static final Logger LOGGER = Logger.getLogger(User.class.getName());
	
	public enum GSMethod { Version, UserId, UserName, Email, RequiresProgress, Organisation, Role, Password; }
	public enum GSOp { get, set; }
	public enum Action { Add,Delete,Update; }
	
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
	
	public int compare(User other) {
		return (new UserComparator()).compare(this,other);
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
				method = this.getClass().getMethod("get"+gsname.name(), String.class);
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
	public Object getUserMethod(GSMethod gsname){
		return callMethod(gsname, GSOp.get, null);
	}
	public Object setUserMethod(GSMethod gsname,String param){
		return callMethod(gsname, GSOp.set, param);
	}
	
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

}