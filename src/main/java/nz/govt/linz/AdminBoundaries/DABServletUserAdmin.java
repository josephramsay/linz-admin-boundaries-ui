package nz.govt.linz.AdminBoundaries;

/**
 * AdminBoundaries
 *
 * Copyright 2014 Crown copyright (c)
 * Land Information New Zealand and the New Zealand Government.
 * All rights reserved
 *
 * This program is released under the terms of the new BSD license. See the
 * LICENSE file for more information.
 */

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.http.*;

import nz.govt.linz.AdminBoundaries.DABFormatterUser.TorP;
import nz.govt.linz.AdminBoundaries.UserAdmin.User;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReader;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReaderAIMS;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReaderPostgreSQL;
import nz.govt.linz.AdminBoundaries.UserAdmin.UserReaderTomcat;

/**
 * Servlet to allow user to edit python dab config file without redeploying
 * @author jramsay
 */
public class DABServletUserAdmin extends DABServlet {
	
	private static final Logger LOGGER = Logger.getLogger(DABServletUserAdmin.class.getName());

	static final long serialVersionUID = 115L;

	public String docType = "<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">\n";

	private UserReaderTomcat urtc;
	private UserReaderPostgreSQL urpg;
	private UserReaderAIMS uraa;
	
	
	/**
	 * Initialise servlet class setting status and getting formatter + connector instances
	 */
	public void init() throws ServletException {
		super.init();
		message = "User Editor for Admin Boundarys";
		description = String.join("\n", "This config provides view/edit functionality for AIMS user administration.");

		urtc = new UserReaderTomcat();
		urpg = new UserReaderPostgreSQL();
		uraa = new UserReaderAIMS(reader);//getServletContext());
	}


	/**
	 * Servlet doGet
	 * @param request
	 * @param response
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String accdectable = "";
		String infomessage = "";
		//String sp = request.getServletPath();//  getServletContext().getRealPath("/");
		
		response.setContentType("text/html; charset=UTF-8");
		PrintWriter out = response.getWriter();

		HttpSession session = request.getSession(true);

		Date created = new Date(session.getCreationTime());
		Date accessed = new Date(session.getLastAccessedTime());
		String user = (String) request.getAttribute("currentSessionUser");
		
		Map<String, Map<String,String>> params = readParameters(request);

		Map<String, String> info = new HashMap<>(); 
		if (!params.isEmpty()) {
			//s = summary, a = 1
			info.put("ACTION","submit");
			info.put("RESULT",params.toString());
			info.put("PARAMS",DABFormatter.formatList(params));
			processActions(params);
		}
		
		List<User> tomcat_userlist = urtc.readUserList();
		List<User> postgres_userlist = urpg.readUserList();
		List<User> aims_userlist = uraa.readUserList();

		String users_tc = DABFormatter.formatTable(
			"Tomcat Users",urtc.transformUserList(tomcat_userlist));
		String users_pg = DABFormatter.formatTable(
			"PostgreSQL Users",urpg.transformUserList(postgres_userlist));
		String users_aa = DABFormatter.formatTable(
			"AIMS Users",uraa.transformUserList(aims_userlist));

		String userform_tc = DABFormatterUser.formatUserForm(
			TorP.Tomcat,tomcat_userlist);
		String userform_pg = DABFormatterUser.formatUserForm(
			TorP.PostgreSQL,postgres_userlist);
		String userform_aa = DABFormatterUser.formatUserForm(
			TorP.AIMS,aims_userlist);

		infomessage = dabf.getInfoMessage(info);
		accdectable = dabf.getBackNav();

		//OUTPUT

		out.println(getHTMLWrapper(
				getBodyContent(
						users_tc,userform_tc,
						users_pg,userform_pg,
						users_aa,userform_aa,
						infomessage,accdectable),
				getBodyFooter(created,accessed,user)
				)
			);
	}
	
	/**
	 * performs the user requested actions
	 * @param params
	 */
	private void processActions(Map<String, Map<String,String>> params) {
		if (params.containsKey("tc")) {
			processTCPG(urtc,params.get("tc"));
			urtc.save();
		}
		else if (params.containsKey("pg")) {
			processTCPG(urpg,params.get("pg"));
			urpg.save();
		}
		else if (params.containsKey("aa")) {
			processAIMS(uraa,params.get("aa"));
			uraa.save();
		}
	}
	
	private void processAIMS(UserReaderAIMS reader,Map<String, String> upra) {
		String ver,uid,uname,org,role,reqprg,email,action;
		ver = uid = uname = org = role = reqprg = email = action = null;
		if (upra.containsKey("uname"))  {uname  = upra.get("uname");}
		if (upra.containsKey("role"))   {role   = upra.get("role");}
		if (upra.containsKey("uid"))    {uid    = upra.get("uid");}
		if (upra.containsKey("ver"))    {ver    = upra.get("ver");}
		if (upra.containsKey("reqprg")) {reqprg = upra.get("reqprg");}
		if (upra.containsKey("email"))  {email  = upra.get("email");}
		if (upra.containsKey("org"))    {org    = upra.get("org");}
		if (upra.containsKey("action")) {action = upra.get("act");}
		//ADD : action=save,user!=existing,role!=null,pass!=null
		//EDIT : action=save,user==existing,role!=null,pass!=null
		if ("save".equals(action) && uname != null && role != null && org != null && reqprg != null && email != null) { 
			if (reader.userExists(uname) && uid != null && ver != null){
				LOGGER.info("Modify user "+uname);
				reader.editUser(ver,uid,uname,email,reqprg,org,role);
			}
			else {
				LOGGER.info("Add user "+uname);
				reader.addUser(uname,email,reqprg,org,role);
			}
		}
		//DEL : action=delete,user=existing
		else if("delete".equals(action) && uname != null && reader.userExists(uname)) {
			LOGGER.info("Delete user "+uname);
			reader.delUser(uname);//ver,uid);
		}
		else {
			LOGGER.warning("Cannot match ["+uname+","+role+","+action+"]");
			return;
		}
	}
	
	private void processTCPG(UserReader reader,Map<String, String> upra) {
		String user,pass,role,action;
		user = pass = role = action = null;
		if (upra.containsKey("user")) {user   = upra.get("user");}
		if (upra.containsKey("pass")) {pass   = upra.get("pass");}
		if (upra.containsKey("role")) {role   = upra.get("role");}
		if (upra.containsKey("act"))  {action = upra.get("act");}
		//ADD : action=save,user!=existing,role!=null,pass!=null
		//EDIT : action=save,user==existing,role!=null,pass!=null
		if ("save".equals(action) && user != null && role != null && pass != null) { 
			if (reader.userExists(user)){
				LOGGER.info("Modify user "+user);
				reader.editUser(user,pass,role);
			}
			else {
				LOGGER.info("Add user "+user);
				reader.addUser(user,pass,role);
			}
		}
		//DEL : action=delete,user=existing
		else if("delete".equals(action) && user != null && reader.userExists(user)) {
			LOGGER.info("Delete user "+user);
			reader.delUser(user);
		}
		else {
			LOGGER.warning("Cannot match ["+user+","+role+","+pass+","+action+"]");
			return;
		}
	}
}
