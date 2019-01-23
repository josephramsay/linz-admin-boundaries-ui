package nz.govt.linz.AdminBoundaries;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.catalina.realm.MessageDigestCredentialHandler;
import org.apache.catalina.realm.DigestCredentialHandlerBase;

/**
 * Absract class for user reading functions incl TC and PG
 * @author jramsay
 *
 */
public abstract class UserReader {

	
	protected List<Map<String,String>> user_list;
	
	/**
	 * Convenience getter for the user map 
	 * @return Map of users and passwords
	 */
	public List<Map<String,String>> getUserList(){
		return user_list;
	}
	
	/**
	 * Convenience method to convert list-map format lists into list-list
	 * (where the top row is the header) for use with the dabformatter
	 * html table formatting function
	 * @param userlist
	 * @return
	 */
	public static List<List<String>> transformUserList(List<Map<String,String>> userlist){
		int row_count = 0;
		List<List<String>> new_userlist = new ArrayList<>();
		for (Map<String,String> row : userlist) {
			List<String> new_row = new ArrayList<>();
			for (String key : row.keySet()) {
				if (row_count == 0) {
					new_row.add(key);
				}
				else {
					new_row.add(row.get(key));
				}
			}
			row_count ++;
			new_userlist.add(new_row);
		}
		return new_userlist;
	}
	/**
	 * Returns a matching user_list entry matching by provided key/value pair
	 * @param key
	 * @param value
	 * @return User list entry
	 */
	private Map<String,String> findInUserList(String key, String value){
		for (Map<String,String> user_entry : user_list){
			if (Objects.equals(user_entry.get(key),value)) {
				return user_entry;
			}
		}
		return null;
	}
	/**
	 * Returns a matching user_list entry matching by provided username
	 * @param username
	 * @return
	 */
	public Map<String,String> findInUserList(String value){
		return findInUserList("username", value);
	}
	public abstract void save();
	
	public abstract void load();
	
	public abstract String encrypt(String pass);
	
	public abstract void saveUserList();
	
	public abstract List<Map<String,String>> readUserList();

	
	public boolean userExists(String username) {
		return findInUserList(username) != null;
	}
	/**
	 * Adds a user pass entry to the user_list and saves the result
	 * @param user Username
	 * @param pass Password unencrypted
	 */
	public void addUser(String username,String password, String roles) {
		Map<String,String> user_entry = new HashMap<>();
		user_entry.put("username", username);
		user_entry.put("password", encrypt(password));
		user_entry.put("roles", roles);
		user_list.add(user_entry);
		saveUserList();
	}
	
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
		if (user_list.remove(findInUserList(username))) {
			addUser(username, password, roles);
		}
	}
}

