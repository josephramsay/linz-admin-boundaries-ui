package nz.govt.linz.AdminBoundaries;

import java.util.List;
import java.util.Arrays;
import nz.govt.linz.AdminBoundaries.UserAdmin.User;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReader;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReaderAIMS;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserAIMS.AARoles;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserPostgreSQL.PGRoles;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserTomcat.TCRoles;


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
	
	public enum TPA {
		Tomcat("tc","Add / Delete Tomcat User",
			"Add or remove users from the tomcat-users.xml file in the AIMS config. "
			+ "Note: Plaintext passwords are encrypted and cannot be recovered once saved",
			UserReader.getNames(TCRoles.class),
			Arrays.asList("LINZ"),
			true,true,false,false,false,true,true),
		PostgreSQL("pg","Add / Delete PostgreSQL User",
			"Add/Remove AIMS access roles for existing users only. "
			+ "Note: This dialog cannot add or remove database users",
			UserReader.getNames(PGRoles.class),
			Arrays.asList("LINZ"),
			true,false, false,false,false,false,false),
		AIMS("aa","Add / Delete AIMS User",
			"AIMS internal user administration. "
			+ "Note: Unless entered email addresses are constructed from username@&lt;org.domain&gt;",
			UserReader.getNames(AARoles.class),
			UserReaderAIMS.getOrgNames(),
			false,false,true,true,true,false,false);
		private final String menuref,title,details;
		private List<String> roles,orgs;
		private final boolean multi, pwbox, embox, orgdd, rpcb, r_tc, r_aa;
		TPA(String menuref,String title, String details, List<String> roles,List<String> orgs,
				boolean multi, boolean pwbox, boolean embox, boolean orgdd, 
				boolean rpcb, boolean r_tc, boolean r_aa
				){
			this.menuref = menuref;
			this.title   = title;
			this.roles   = roles;
			this.orgs    = orgs;
			this.multi   = multi;
			this.pwbox   = pwbox;
			this.embox   = embox;
			this.orgdd   = orgdd;
			this.rpcb    = rpcb;
			this.r_tc    = r_tc;
			this.r_aa    = r_aa;
			this.details = details;
		}
	}; 
	
	private static final String WARN1 = "CAUTION! This restarts Tomcat. All Users will be disconnected!";
	private static final String WARN2 = "CAUTION! This restarts the DAB Application. Unsaved data will be lost.";
	
	/**
	 * Reformats a list/list/string as an html table with the caption tname
	 * @param fname
	 * @param config
	 * @return
	 */	
	public static String formatUserForm(TPA tpa, List<User> userlist) {
		String form = "";
		form += "<article><form method=\"post\">\n";
		form += "<legend>"+tpa.title+"</legend>\n";
		form += getUserDropDown(tpa.menuref,userlist);
		form += getRoleDropDown(tpa.menuref,tpa.roles,tpa.multi);
		if (tpa.pwbox)   {form += getPasswordEntry(tpa.menuref);};
		if (tpa.embox)   {form += getEmailEntry(tpa.menuref);};
		if (tpa.orgdd)   {form += getOrganisationDropDown(tpa.menuref,tpa.orgs);};
		if (tpa.rpcb)    {form += getRequiresProgressCheckBox(tpa.menuref);};
    	form += "<section><input type=\"submit\" name=\""+tpa.menuref+"_act\" value=\"save\"/></section>";
    	form += "<section><input type=\"submit\" name=\""+tpa.menuref+"_act\" value=\"delete\"/></section>";
    	if (tpa.r_aa) {form += getRestartButton(tpa.menuref,"restart aa",WARN2);};
    	if (tpa.r_tc) {form += getRestartButton(tpa.menuref,"restart tc",WARN1);};
	    form += "</form><br/>\n";
	    form += "<details>\n<summary>"+tpa.title+" Summary</summary>\n";
	    form += "<p>"+tpa.details+"</p>\n</details>\n";
	    form += "</article>\n";
	    return form;
	}


	private static String getUserDropDown(String menuref,List<User> userlist) {
		String form = "<div><label class=\"sec\">Username</label>&nbsp;\n";
		form += "<select name=\""+menuref+"_user\" class=\"multiselect\">\n";
		for (User user : userlist) {
			form += "<option value=\""+user.getUserName()+"\">"+user.getUserName()+"</option>\n";
		}
		form += "</select></div>&nbsp;\n";

		return form;
	}

	private static String getRoleDropDown(String menuref, List<String> rolelist, boolean multi) {//List<User> userlist) {
		String form = "<div><label class=\"sec\">Role</label>&nbsp;\n";
		form += "<select name=\""+menuref+"_role\" class=\"multiselect2\" "+(multi?"multiple":"")+">\n";
		for (String role : rolelist) {//consolidateRoles(userlist)) {
			form += "<option value=\""+role+"\">"+role+"</option>\n";
		}
		form += "</select></div>&nbsp;\n";
		return form;
	}
	
	private static String getPasswordEntry(String menuref) {
		String form = "<div><label class=\"sec\">Password</label>&nbsp;\n";
		form += "<input name=\""+menuref+"_pass\" class=\"sec\" value=\"\" type=\"text\"/></div><br/>\n";
		return form;
	}
	
	private static String getOrganisationDropDown(String menuref,List<String> orglist) {
		String form = "<div><label class=\"sec\">Organisation</label>&nbsp;\n";
		form += "<select name=\""+menuref+"_org\" class=\"multiselect2\">\n";
		for (String role : orglist) {
			form += "<option value=\""+role+"\">"+role+"</option>\n";
		}
		form += "</select></div>&nbsp;\n";
		return form;
	}
	
	private static String getRequiresProgressCheckBox(String menuref) {
		String form = "</br>\n<div><label class=\"sec\">Requires&nbsp;Progress</label>&nbsp;\n";
		form += "<input type=\"checkbox\" name=\""+menuref+"_rp\" class=\"sec\" value=\"reqprg\"></div>\n";
		return form;
	}
	
	private static String getEmailEntry(String menuref) {
		String form = "<div><label class=\"sec\">Email</label>&nbsp;\n";
		form += "<input name=\""+menuref+"_email\" class=\"sec\" value=\"\" type=\"text\"/></div>&nbsp;\n";
		return form;
	}
	
	private static String getRestartButton(String menuref,String val,String tt) {
		return "<section><input title=\""+tt+"\"type=\"submit\" name=\""+menuref+"_act\" value=\""+val+"\"/></section>";
	}
	
}

