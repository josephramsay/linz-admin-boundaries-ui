package nz.govt.linz.AdminBoundaries;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Formatter class specifically for users incorporating user-view (as a table). 
 * Under this a subform with a user dropdown with del/add/update function buttons
 * @author jramsay
 *
 */
public class DABFormatterUser extends DABFormatter {
	
	
	public DABFormatterUser() {
		super();
	}
	
	public enum TorP {
		Tomcat("tc","Add / Delete Tomcat User",true), 
		PostgreSQL("pg","Add / Delete PostgreSQL User",false),
		AIMS("aa","Add / Delete AIMS User",false);
		private final String menuref,title;
		private final boolean pwbox;
		TorP(String menuref,String title, boolean pwbox){
			this.menuref = menuref;
			this.title = title;
			this.pwbox = pwbox;
		}
	}; 
	
	/**
	 * Reformats a list/list/string as an html table with the caption tname
	 * @param fname
	 * @param config
	 * @return
	 */	
	public static String formatUserForm(TorP torp, List<User> userlist) {
		String form = "";
		form += "<article><form method=\"post\">\n";
		form += "<legend>"+torp.title+"</legend>\n";
		form += getUserDropDown(torp.menuref,userlist);
		form += getRoleDropDown(torp.menuref,userlist);
		if (torp.pwbox) {form += getPasswordEntry(torp.menuref);};
    	form += "<section><input type=\"submit\" name=\""+torp.menuref+"_act\" value=\"save\"/></section>";
    	form += "<section><input type=\"submit\" name=\""+torp.menuref+"_act\" value=\"delete\"/></section>";
	    form += "</form>\n</article>\n";
	    return form;
	}


	private static String getUserDropDown(String menuref,List<User> userlist) {
		String form = "<label class=\"sec\">Username</label>&nbsp;\n";
		form += "<select name=\""+menuref+"_user\" class=\"multiselect\">\n";
		for (User user : userlist) {
			form += "<option value=\""+user.getUserName()+"\">"+user.getUserName()+"</option>\n";
		}
		form += "</select>\n";

		return form;
	}


	private static String getRoleDropDown(String menuref,List<User> userlist) {
		String form = "<label class=\"sec\">Role</label>&nbsp;\n";
		form += "<select name=\""+menuref+"_role\" class=\"multiselect\" multiple>\n";
		for (String role : consolidateRoles(userlist)) {
			form += "<option value=\""+role+"\">"+role+"</option>\n";
		}
		form += "</select>\n";
		return form;
	}
	
	private static String getPasswordEntry(String menuref) {
		String form = "<label class=\"sec\">Password</label>&nbsp;\n";
		form += "<input name=\""+menuref+"_pass\" value=\"\" type=\"text\"/><br/>\n";
		return form;
	}
	
	/**
	 * For mutiple roles expressed as a comma sep list, split them and add to set
	 * @param userlist
	 * @return
	 */
	private static Set<String> consolidateRoles(List<User> userlist){
		Set<String> rolelist = new HashSet<>();
		for (User user : userlist) {
			for (String rolestr : user.getSpringRolls()) {
				rolelist.add(rolestr);
			}
		}
		return rolelist;
	}
	
}

