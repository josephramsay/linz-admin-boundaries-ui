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


	static final long serialVersionUID = 1;

	/** Formatter class for converting data-maps to html strings */
	private DABFormatter dabf;

	/** Class holding info on tables for comparson */
	private DABContainerComp ccomp;

	public String docType = "<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">\n";

	/**
	 * Initialise servlet class setting status and getting formatter + connector instances
	 */
	public void init() throws ServletException {
		super.init();
		message = "Config Editor for Admin Boundarys";
		description = String.join("\n", "This config editor sets options for the background Admin Boundaries processing script.",
				"<br/><u>DATABASE</u>",
				"host:: the database host name",
				"name:: the Database name (linz_db)",
				"rolename:: the User to run downloader functions as (this user will own the created tables) (bde_dba)",
				"user:: the user to login as (must be a member of unix_logins) (dab_user)",
				"password:: login user password",
				"port:: database port (5432)",
				"schema:: temporary schema where imported tables are stored (admin_bdys_import)",
				"originschema:: schema where admin boundaries are saved (admin_bdys)",
				"<br/><u>USER</u>",
				"list:: comma seperated list of usernames that will be notified when import completes",
				"domain:: email list domain name (linz.govt.nz)",
				"smtp:: SMTP server",
				"link:: = link provided to users in notification",
				"<br/><u>LAYER</u>",
				"name:: name of table to apply grid function to (territorial_authority)",
				"output_srid:: SRID to transfor imported layers to (4167)",
				"geom_column:: grid able geom column name (shape)",
				"create_grid:: Enable grid creation T/F (True)",
				"grid_res:: Grid resolution (0.05)",
				"shift_geometry:: enable geom shift function. <i>needed to include cross dateline locations</i> (True)",
				"<br/><u>CONNECTION</u>",
				"ftphost:: FTP host to connect to for StatsNZ data",
				"ftpport:: FTP port (22)",
				"ftpuser:: FTP login/user",
				"ftppass:: FTP password",
				"ftppath:: Path from FTP root to search for files",
				"<br/><u>MESHBLOCK</u>",
				"filepattern:: Regular expression matching meshblock file",
				"localpath:: Temporary path to save downloaded files (/tmp)",
				"colmap:: list of table reformatting rules <i>see below</i>",
				"<br/><u>NZLOCALITIES</u>",
				"filepath:: (path to localities share)",
				"filename:: (name of localities file)",
				"colmap:: = list of table reformatting rules <i>see below</i>",
				"<br/><u>VALIDATION <i>partially implemented, for future development</i></u>",
				"test:: [[(test query) , (expected result)],[]],",
				"data:: [[(test query) , (expected result)],[]],",
				"spatial:: [[(test query) , (expected result)],[]]",
				"<br/><u>COLMAP Format</u>",
				"( file1 ) : ",
				"   { table1 : ( tablename ),",
				"     rename : [ { old : ( old column name ), new : ( new column name ) }, {} ],",
				"     add : [ { add : ( column to add ), type : ( type of added column ) } ],",
				"     drop : [ ( column to drop ), () ],",
				"     primary : ( primary key column name ),",
				"     geom : ( geometry column name ),",
				"     srid : ( spatial reference ),",
				"     grid : { geocol : (geometry colum name ), res : ( grid resolution ) },",
				"     cast : [ { cast: ( column name to re cast ), type : ( data type to cast column to ) }, {} ],",
				"     permission : [(user with schema access permissions) , () ]",
				"   },",
				"  { table2 : ... },",
				"( file2 ) : {}");

		dabf = new DABFormatter();
		ccomp = new DABContainerComp();

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
		Map<String, Map<String,String>> config = new LinkedHashMap<>();
		Enumeration<?> params = request.getParameterNames(); 
		while (params.hasMoreElements()) {
			String pname = (String) params.nextElement();
			String[] parts = pname.split("_");
			if (config.containsKey(parts[0])) {
				config.get(parts[0]).put(parts[1], request.getParameterValues(pname)[0]);
			}
			else {
				config.put(parts[0], new LinkedHashMap<String,String>(){
					private static final long serialVersionUID = 1L;
					{put(parts[1],request.getParameterValues(pname)[0]);}});
			}
		}

		Map<String, String> info = new HashMap<>(); 

		if (!config.isEmpty()) {
			//s = summary, a = 1
			info.put("ACTION","submit");
			info.put("RESULT",config.toString());
			ccomp.setConfig(config);
			configform = DABFormatter.formatForm("DAB Configuration",config);
		}
		else {
			configform = DABFormatter.formatForm("DAB Configuration (saved)",ccomp.getConfig());
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
