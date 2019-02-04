package nz.govt.linz.AdminBoundaries.UserAdmin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import nz.govt.linz.AdminBoundaries.UserAdmin.User.GSMethod;

import java.lang.reflect.Method;

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
	
	
	private static Map<String,Method> getReadMethods(User user) {
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
	 * Convenience method to convert list-map format lists into list-list
	 * (where the top row is the header) for use with the dabformatter
	 * html table formatting function
	 * @param userlist
	 * @return
	 */
	public static List<List<String>> transformUserList_OLD(List<Map<String,String>> userlist){
		int row_count = 0;
		List<List<String>> new_userlist = new ArrayList<>();
		for (Map<String,String> row : userlist) {
			List<String> new_row = new ArrayList<>();
			for (String key : row.keySet()) {
				if (row_count == 0) { new_row.add(key); }
				else { new_row.add(row.get(key)); }
			}
			row_count ++;
			new_userlist.add(new_row);
		}
		return new_userlist;
	}	
	
	public static List<List<String>> transformUserList(List<User> userlist){
		int row_count = 0;
		List<List<String>> new_userlist = new ArrayList<>();
		for (User user : userlist) {
			List<String> new_row = new ArrayList<>();
			for (String key : User.getReadMethods(user).keySet()) {
				if (row_count == 0) { new_row.add(key); }
				else { new_row.add((String) user.getUserMethod(GSMethod.valueOf(key))); }
			}
			row_count ++;
			new_userlist.add(new_row);
		}
		return new_userlist;
	}
	
	
	/**
	 * Returns a matching user_list entry matching by provided key/value pair
	 * @param key eg "userName"
	 * @param value eg "JSmith"
	 * @return User list entry
	 */
	private User findInUserList(String key, String value){
		for (User user : user_list){
			if (Objects.equals(user.getUserMethod(user.matchFieldName(key)),value)) {
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
		return findInUserList("username", value);
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
		if (user_list.contains(user)) {
			LOGGER.warning("Cannot add user "+user+". Already exists. Updating instead.");
			editUser(user);
		}
		else {
			user_list.add(user);
			saveUserList();
		}
	}
	public void delUser(User user) {
		if (user_list.contains(user)) {
			user_list.remove(user);
			saveUserList();
		}
		else {
			LOGGER.warning("Cannot delete user "+user+". Does not exist");
		}
	}
	
	public void editUser(User user) {
		User user_old = user_list.get(user_list.indexOf(user));
		user_old.merge(user);
		saveUserList();
	}

	/* user string functions */
	
	/**
	 * Replaces the user/pass combo in the user_list and saves.
	 * Only adds a replacement if the original exists
	 * @param user Username
	 * @param pass Password unencrypted
	 */
	public abstract void addUser(String username,String password, String roles);
	
	/**
	 * Removes the selected user from the user_list and saves
	 * @param user Username key
	 */
	public void delUser(String username) {
		user_list.remove(findInUserList(username));
		saveUserList();
	}
	
	/**
	 * Replaces the user/pass combo in the user_list and saves.
	 * Only adds a replacement if the original exists
	 * @param user Username
	 * @param pass Password unencrypted
	 */
	public void editUser(String username,String password, String roles) {
		User user = findInUserList(username);
		if (user_list.remove(user)) {
			addUser(user);
		}
	}
}

