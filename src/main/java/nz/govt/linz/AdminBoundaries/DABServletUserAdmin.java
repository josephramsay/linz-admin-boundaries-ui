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

import javax.servlet.*;
import javax.servlet.http.*;

import nz.govt.linz.AdminBoundaries.DABFormatterUser.TorP;

/**
 * Servlet to allow user to edit python dab config file without redeploying
 * @author jramsay
 */
public class DABServletUserAdmin extends DABServlet {


	static final long serialVersionUID = 115L;

	public String docType = "<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">\n";

	private UserReaderTomcat urtc;
	private UserReaderPostgreSQL urpg;
	
	
	/**
	 * Initialise servlet class setting status and getting formatter + connector instances
	 */
	public void init() throws ServletException {
		super.init();
		message = "User Editor for Admin Boundarys";
		description = String.join("\n", "This config provides view/edit functionality for AIMS user administration.");
		urtc = new UserReaderTomcat();
		urpg = new UserReaderPostgreSQL();
	}


	/**
	 * Servlet doGet
	 * @param request
	 * @param response
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		String configform = "";
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
		}
		
		List<Map<String,String>> tomcat_userlist = urtc.readUserList();
		List<Map<String,String>> postgres_userlist = urpg.readUserList();
		
		String users_tc = DABFormatter.formatTable(
			"Tomcat Users",UserReader.transformUserList(tomcat_userlist));
		String users_pg = DABFormatter.formatTable(
			"PostgreSQL Users",UserReader.transformUserList(postgres_userlist));

		String userform_tc = DABFormatterUser.formatUserForm(
			TorP.Tomcat,tomcat_userlist);
		String userform_pg = DABFormatterUser.formatUserForm(
			TorP.PostgreSQL,postgres_userlist);

		infomessage = dabf.getInfoMessage(info);
		accdectable = dabf.getBackNav();

		//OUTPUT

		out.println(getHTMLWrapper(
				getBodyContent(
						users_tc,userform_tc,
						users_pg,userform_pg,
						infomessage,accdectable),
				getBodyFooter(created,accessed,user)
				)
			);
	}
}
