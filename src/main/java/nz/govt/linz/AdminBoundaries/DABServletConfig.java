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

/**
 * Servlet to allow user to edit python dab config file without redeploying
 * @author jramsay
 */
public class DABServletConfig extends DABServlet {


	static final long serialVersionUID = 105L;

	public String docType = "<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">\n";

	/**
	 * Initialise servlet class setting status and getting formatter + connector instances
	 */
	public void init() throws ServletException {
		super.init();
		message = "Config Editor for Admin Boundarys";
		description = "This config editor sets options for the background Admin Boundaries processing script.";
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

		/** check parameters list, if anything parse, store and redisplay */
		Map<String, Map<String,String>> config = readParameters(request);

		Map<String, String> info = new HashMap<>(); 
		if (!config.isEmpty()) {
			//s = summary, a = 1
			info.put("ACTION","submit");
			info.put("RESULT",config.toString());
			ccomp.setConfig(config);
			info.put("FORM", DABFormatter.formatForm("DAB Configuration (new)",config,ccomp.getConfigDescriptions()));
		}
		else {
			configform = DABFormatter.formatForm("DAB Configuration (read)",ccomp.getConfig(),ccomp.getConfigDescriptions());
		}
		
		infomessage = dabf.getInfoMessage(info);
		accdectable = dabf.getBackNav();

		//OUTPUT

		out.println(getHTMLWrapper(
				getBodyContent(infomessage,configform,accdectable),
				getBodyFooter(created,accessed,user)
				)
			);
	}
}
