package nz.govt.linz.AdminBoundaries;

import java.util.Arrays;
import java.util.List;
import nz.govt.linz.AdminBoundaries.UserAdmin.User;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReader;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserAIMS.AARoles;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserPostgreSQL.PGRoles;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserTomcat.TCRoles;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserAIMS.Organisation;


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
		Tomcat("tc","Add / Delete Tomcat User",
			"Add or remove users from the tomcat-users.xml file in the AIMS config. "
			+ "Note: Plaintext passwords are encrypted and cannot be recovered once saved",
			UserReader.getNames(TCRoles.class),true,true,false,false,false),
		PostgreSQL("pg","Add / Delete PostgreSQL User",
			"Add/Remove AIMS access roles for existing users only. "
			+ "Note: This dialog cannot add or remove database users",
			UserReader.getNames(PGRoles.class),true,false, false,false,false),
		AIMS("aa","Add / Delete AIMS User",
			"AIMS internal user administration. "
			+ "Note: Unless entered email addresses are constructed from username@&lt;org.domain&gt;",
			UserReader.getNames(AARoles.class),false,false,true,true,true);
		private final String menuref,title,details;
		private List<String> roles;
		private final boolean multi,pwbox, embox, orgdd, rpcb;
		TorP(String menuref,String title, String details, List<String> roles,
				boolean multi, boolean pwbox, boolean embox, boolean orgdd, boolean rpcb
				){
			this.menuref = menuref;
			this.title   = title;
			this.roles   = roles;
			this.multi   = multi;
			this.pwbox   = pwbox;
			this.embox   = embox;
			this.orgdd   = orgdd;
			this.rpcb    = rpcb;
			this.details = details;
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
		form += getRoleDropDown(torp.menuref,torp.roles,torp.multi);
		if (torp.pwbox) {form += getPasswordEntry(torp.menuref);};
		if (torp.embox) {form += getEmailEntry(torp.menuref);};
		if (torp.orgdd) {form += getOrganisationDropDown(torp.menuref,UserReader.getNames(Organisation.class));};
		if (torp.rpcb)  {form += getRequiresProgressCheckBox(torp.menuref);};
    	form += "<section><input type=\"submit\" name=\""+torp.menuref+"_act\" value=\"save\"/></section>";
    	form += "<section><input type=\"submit\" name=\""+torp.menuref+"_act\" value=\"delete\"/></section>";
	    form += "</form><br/>\n";
	    form += "<details>\n<summary>"+torp.title+" Summary</summary>\n";
	    form += "<p>"+torp.details+"</p>\n</details>\n";
	    form += "</article>\n";
	    return form;
	}


	private static String getUserDropDown(String menuref,List<User> userlist) {
		String form = "<label class=\"sec\">Username</label>&nbsp;\n";
		form += "<select name=\""+menuref+"_user\" class=\"multiselect\">\n";
		for (User user : userlist) {
			form += "<option value=\""+user.getUserName()+"\">"+user.getUserName()+"</option>\n";
		}
		form += "</select>&nbsp;\n";

		return form;
	}

	private static String getRoleDropDown(String menuref, List<String> rolelist, boolean multi) {//List<User> userlist) {
		String form = "<label class=\"sec\">Role</label>&nbsp;\n";
		form += "<select name=\""+menuref+"_role\" class=\"multiselect\" "+(multi?"multiple":"")+">\n";
		for (String role : rolelist) {//consolidateRoles(userlist)) {
			form += "<option value=\""+role+"\">"+role+"</option>\n";
		}
		form += "</select>&nbsp;\n";
		return form;
	}
	
	private static String getPasswordEntry(String menuref) {
		String form = "<label class=\"sec\">Password</label>&nbsp;\n";
		form += "<input name=\""+menuref+"_pass\" value=\"\" type=\"text\"/><br/>\n";
		return form;
	}
	
	private static String getOrganisationDropDown(String menuref,List<String> orglist) {
		String form = "<label class=\"sec\">Organisation</label>&nbsp;\n";
		form += "<select name=\""+menuref+"_org\" class=\"multiselect\">\n";
		for (String role : orglist) {
			form += "<option value=\""+role+"\">"+role+"</option>\n";
		}
		form += "</select>&nbsp;\n";
		return form;
	}
	
	private static String getRequiresProgressCheckBox(String menuref) {
		String form = "</br></br><label class=\"sec\">Requires&nbsp;Progress</label>&nbsp;\n";
		form += "<input type=\"checkbox\" name=\""+menuref+"_rp\" class=\"sec\" value=\"reqprg\">\n";
		return form;
	}
	
	private static String getEmailEntry(String menuref) {
		String form = "<label class=\"sec\">Email</label>&nbsp;\n";
		form += "<input name=\""+menuref+"_email\" class=\"sec\" value=\"\" type=\"text\"/>&nbsp;\n";
		return form;
	}
	
}

