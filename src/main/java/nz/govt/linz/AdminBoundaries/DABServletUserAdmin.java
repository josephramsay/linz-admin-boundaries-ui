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
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.servlet.*;
import javax.servlet.http.*;

import nz.govt.linz.AdminBoundaries.DABFormatterUser.TPA;
import nz.govt.linz.AdminBoundaries.UserAdmin.User;
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
		description = String.join("\n", 
			"This page provides view/edit functionality for AIMS user administration. " 
			+ "Users are linked by the primary identifier; 'username'. All identities "
			+ "must be synchronised on this key for AIMS access.",
			"The 'Tomcat' user list presents the contents of the tomcat-users.xml file "
			+ "which tomcat uses to provide a secure login mechanism for access to the webserver. "
			+"Passwords are encrypted after being entered and saved in their encrypted format. They cannot be decrypted",
			"The 'PostgreSQL' dialog enables the granting/revocation of access to the AIMS database tables "
			+ "using AIMS database group roles. Users cannot be added or deleted from the "
			+ "database using his dialog.",
			"The 'AIMS' dialog grants users AIMS specifc access via the API with permissions "
			+"defined by AIMS specific categories; Follower/Reviewer/Administrator/Publisher.");

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
		String user = request.getRemoteUser().toString();
		//String user = request.getUserPrincipal().toString();
		
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
		
		String users_tc = DABFormatter.formatTable( "Tomcat Users",
			urtc.transformUserList(tomcat_userlist));
		String users_pg = DABFormatter.formatTable( "PostgreSQL Users",
			urpg.transformUserList(postgres_userlist));
		String users_aa = DABFormatter.formatTable( "AIMS Users",
			uraa.transformUserList(aims_userlist));

		String userform_tc = DABFormatterUser.formatUserForm(
			TPA.Tomcat,tomcat_userlist);
		String userform_pg = DABFormatterUser.formatUserForm(
			TPA.PostgreSQL,postgres_userlist);
		String userform_aa = DABFormatterUser.formatUserForm(
			TPA.AIMS,aims_userlist);

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
			processTomcat(urtc,params.get("tc"));
			urtc.save();
		}
		else if (params.containsKey("pg")) {
			processPostgreSQL(urpg,params.get("pg"));
			urpg.save();
		}
		else if (params.containsKey("aa")) {
			processAIMS(uraa,params.get("aa"));
			uraa.save();
		}
	}
	
	private void processAIMS(UserReaderAIMS reader,Map<String, String> upra) {
		String user,org,role,rp,email,action;
		user = org = role = rp = email = action = null;
		if (upra.containsKey("user"))  { user  = upra.get("user"); }
		if (upra.containsKey("role"))  { role   = upra.get("role"); }
		if (upra.containsKey("rp"))    { rp = "true"; }
		else                           { rp = "false"; }
		if (upra.containsKey("email")) { email  = upra.get("email"); }
		if (upra.containsKey("org"))   { org    = upra.get("org"); }
		if (upra.containsKey("act"))   { action = upra.get("act"); }
		//ADD : action=save,user!=existing,role!=null,pass!=null
		//EDIT : action=save,user==existing,role!=null,pass!=null
		boolean exists = reader.userExists(user);
		if ("save".equals(action) && user != null && role != null && org != null && rp != null && email != null) { 
			if (exists){
				LOGGER.info("Modify AA user "+user);
				reader.editUser(user, email, rp, org, role);
			}
			else {
				LOGGER.info("Add AA user "+user);
				reader.addUser(user, email, rp, org, role);
			}
		}
		//DEL : action=delete,user=existing
		else if("delete".equals(action) && user != null && exists) {
			LOGGER.info("Delete AA user "+user);
			reader.delUser(user);//ver,uid);
		}
		else {
			LOGGER.warning("Cannot match [u:"+user+"/"+(exists?"E":"x")+",r:"+role+",p:"+rp+",e:"+email+",o:"+org+",a:"+action+"]");
			return;
		}
	}
	
	private void processTomcat(UserReaderTomcat reader,Map<String, String> upra) {
		String user,pass,role,action;
		user = pass = role = action = null;
		if (upra.containsKey("user")) {user   = upra.get("user");}
		if (upra.containsKey("pass")) {pass   = upra.get("pass");}
		if (upra.containsKey("role")) {role   = upra.get("role");}
		if (upra.containsKey("act"))  {action = upra.get("act");}
		//ADD : action=save,user!=existing,role!=null,pass!=null
		//EDIT : action=save,user==existing,role!=null,pass!=null
		boolean exists = reader.userExists(user);
		if ("save".equals(action) && user != null && role != null) { 
			if (exists){
				LOGGER.info("Modify TC user "+user);
				reader.editUser(user,pass,role);
			}
			else if (pass != null) {
				LOGGER.info("Add TC user "+user);
				reader.addUser(user,pass,role);
			}
		}
		//DEL : action=delete,user=existing
		else if("delete".equals(action) && user != null && exists) {
			LOGGER.info("Delete TC user "+user);
			reader.delUser(user);
		}
		else if("restart aa".equals(action)) {
			restartAIMS();
		}
		else if("restart tc".equals(action)) {
			restartTomcat();
		}
		else {
			LOGGER.warning("Cannot match [u:"+user+"/"+(exists?"E":"x")+",r:"+role+",p:"+pass+",a:"+action+"]");
			return;
		}
	}
	
	private void processPostgreSQL(UserReaderPostgreSQL reader,Map<String, String> upra) {
		String user,role,action;
		user = role = action = null;
		if (upra.containsKey("user")) {user   = upra.get("user");}
		if (upra.containsKey("role")) {role   = upra.get("role");}
		if (upra.containsKey("act"))  {action = upra.get("act");}
		//ADD : action=save,user!=existing,role!=null,pass!=null
		//EDIT : action=save,user==existing,role!=null,pass!=null
		boolean exists = reader.userExists(user);
		LOGGER.info("UL:"+reader.getUserList().toString());
		if ("save".equals(action) && user != null && role != null) { 
			if (exists){
				LOGGER.info("Modify PG user "+user);
				reader.editUser(user,role);
			}
			else {
				LOGGER.info("Add PG user "+user);
				reader.addUser(user,role);
			}
		}
		//DEL : action=delete,user=existing
		else if("delete".equals(action) && user != null && exists) {
			LOGGER.info("Delete PG user "+user);
			reader.delUser(user);
		}
		else {
			LOGGER.warning("Cannot match [u:"+user+"/"+(exists?"E":"x")+",r:"+role+",a:"+action+"]");
			return;
		}
	}
	
	/**
	 * Brutal restart for the whole tomcat instance.
	 */
	private void restartTomcat() {
		/*
		try (Socket clientSocket = new Socket("localhost", 8005)){;
		clientSocket.getOutputStream().write("RESTART".getBytes());
		clientSocket.getOutputStream().close();
		clientSocket.close();
		}
		catch (IOException ioe) {
			LOGGER.warning("Cannot restart Tomcat. "+ioe);
		}
		*/
		try {
			Runtime.getRuntime().exec("/opt/tomcat8/bin/restart.sh");
			TimeUnit.SECONDS.sleep(5);
		} catch (IOException ioe) {
			LOGGER.warning("Cannot restart Tomcat. "+ioe);
		} catch (InterruptedException ie) {
			LOGGER.warning("Interrupted sleep on Tomcat restart. "+ie);
		}
		
	}
	
	/**
	 * Restart AIMS webapp
	 */
	private void restartAIMS() {
		String identifier = "Catalina:j2eeType=WebModule,name=//localhost/aims,J2EEApplication=none,J2EEServer=none";
		MBeanServer mbs = MBeanServerFactory.findMBeanServer(null).get(0);
		try {
			ObjectName objectName = new ObjectName(identifier);
			LOGGER.info("Attempting AIMS restart");
			mbs.invoke(objectName, "reload", null, null);
		}
		catch (MalformedObjectNameException|ReflectionException|InstanceNotFoundException|MBeanException multi) {
			LOGGER.warning("Cannot restart AIMS. "+multi);
			LOGGER.warning("Attempting Tomcat restart");
			restartTomcat();
		}
		
	}
}
