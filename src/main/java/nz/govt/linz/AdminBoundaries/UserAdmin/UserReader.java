package nz.govt.linz.AdminBoundaries.UserAdmin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.EnumSet;

/**
 * Absract class for user reading functions incl TC and PG
 * @author jramsay
 *
 */
public abstract class UserReader {
	
	private static final Logger LOGGER = Logger.getLogger( UserReader.class.getName() );
	
	protected List<User> user_list;
	
	/**
	 * Convenience getter for the user map 
	 * @return Map of users and passwords
	 */
	public List<User> getUserList(){
		return user_list;
	}
	

	/**
	 * Transforms a list of users into a "row" of strings by calling their 
	 * GSMethod approved getter methods 
	 * @param userlist
	 * @return
	 */
	public List<List<String>> transformUserList(List<? extends User> userlist){
		boolean first = true;
		List<List<String>> new_userlist = new ArrayList<>();
		List<String> top_row = new ArrayList<>();
		for (User user : userlist) {
			//LOGGER.info("transform U:"+user);
			//LOGGER.info("transform gGSM:"+user.getGSMethod().toString());
			///for (String s : user.getGSMethod()) {LOGGER.info("transform user gGSM:"+s);}
			List<String> new_row = new ArrayList<>();
			Map<String,Method> getters = User.readMethods(user);
			Iterable<String> giter = getters
				.keySet()
				.stream()
				.filter(x -> user.getGSMethod().contains(x))::iterator;
				//.filter(x -> isInEnum(x,GSMethod.class))::iterator;
			for (String key : giter) {
				//LOGGER.info("transform  K:"+key);
				if (first) {
					//add the header-row/column-names
					top_row.add(key); 
				} 
				Object o = user.invokeMethod(getters.get(key));
				//LOGGER.info("transform  O:"+String.valueOf(o));
				new_row.add(trimB(o)); 
			}
			if (first) {
				first = false;
				new_userlist.add(top_row);
			}
			new_userlist.add(new_row);
		}
		return new_userlist;
	}
	
	/** Simple function to trim leading/trailing [,] chars from enum.strings */
	private static String trimB(Object o) {
		if (o instanceof EnumSet) { return String.valueOf(o).replaceFirst("^(\\[)(.*)(\\])$","$2");	} 
		else { return String.valueOf(o); }
	}
	
	/**
	 * Checks whether a value exists in an enum based on its name string 
	 * @param value
	 * @param enumClass
	 * @return
	 */
	public <E extends Enum<E>> boolean isInEnum(String value, Class<E> enumClass) {
		  for (E e : enumClass.getEnumConstants()) {
		    if(e.name().equals(value)) { return true; }
		  }
		  return false;
	}
	
	public static String[] getNames2(Class<? extends Enum<?>> e) {
		return Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new);
	}
	
	public static List<String> getNames(Class<? extends Enum<?>> e) {
		return Stream.of(e.getEnumConstants()).map(Enum::name).collect(Collectors.toList()); 
	}	
	
	/**
	 * Returns a matching user_list entry matching by provided key/value pair
	 * @param key eg "userName"
	 * @param value eg "JSmith"
	 * @return User list entry
	 */
	private User findInUserList(String key, String value){
		for (User user : user_list){
			if (Objects.equals(user.readUserAttribute(key),value)) {
				return user;
			}
		}
		return null;
	}
	
	/**
	 * Returns a matching user_list entry matching by provided username
	 * @param username
	 * @return
	 */
	public User findInUserList(String value){
		return findInUserList("UserName", value);
	}
	
	/**
	 * Match the provided user with one from the user list and return
	 * @param user
	 * @return
	 */
	public User findInUserList(User user) {
		if (user_list.contains(user)) {return user;}
		return null;
	}
	
	/**
	 * Low level copy of user_list structure
	 * @return Copy of the user list
	 */
	public abstract List<User> cloneUserList();
	public abstract void save();
	
	public abstract void load();
	
	public abstract String encrypt(String pass);
	
	public abstract void saveUserList();
	
	public abstract List<User> readUserList();

	
	public boolean userExists(String username) {
		return findInUserList(username) != null;
	}

	/* user obj functions */
	public void addUser(User user) {
		if (findInUserList(user.getUserName()) != null) {
			LOGGER.warning("Cannot add user "+user+". Already exists. Updating instead.");
			editUser(user);
		}
		else {
			user_list.add(user);
			saveUserList();
		}
	}
	
	/**
	 * Shortcut to delete which finds existing user in list with highest ver 
	 * @uname User name
	 */
	public void delUser(User user) {
		if (user_list.contains(user)) {
			user_list.remove(user);
			saveUserList();
		}
		else {
			LOGGER.warning("Cannot delete user "+user+". Does not exist");
		}
	}
	
	/**
	 * Edit user(user) calls the common merge function  
	 * @param user
	 */
	public void editUser(User user) {
		//User user_old = user_list.get(user_list.indexOf(user));
		User user_old = findInUserList("UserName",user.getUserName());
		LOGGER.info("Merge "+user_old+"<-"+user);
		user_old.merge(user);
		saveUserList();
	}

	/* user string functions (only del has common args) */

	/**
	 * Removes the selected user from the user_list and saves
	 * @param user Username key
	 */
	public void delUser(String username) {
		delUser(findInUserList(username));
	}
	

}

