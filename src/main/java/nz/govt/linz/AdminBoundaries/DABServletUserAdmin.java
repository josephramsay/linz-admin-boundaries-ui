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
		uraa = new UserReaderAIMS();//getServletContext());
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
			"Tomcat Users",UserReader.transformUserList(tomcat_userlist));
		String users_pg = DABFormatter.formatTable(
			"PostgreSQL Users",UserReader.transformUserList(postgres_userlist));
		String users_aa = DABFormatter.formatTable(
			"AIMS Users",UserReader.transformUserList(aims_userlist));

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
		UserReader reader = null;
		Map<String, String> upra = null;
		if (params.containsKey("tc")) {
			reader = urtc;
			upra = params.get("tc");
		}
		else if (params.containsKey("pg")) {
			reader = urpg;
			upra = params.get("pg");
		}
		else return;

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
				//TODO Fix adduser
				//reader.addUser(user,pass,role);
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
		reader.save();
	}
}
