package nz.govt.linz.AdminBoundaries;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

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

import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class DABServlet extends HttpServlet {
	
	static final long serialVersionUID = 1;
	protected String message;
	protected String title;	
	protected String description;	
	public String hostname;
	
	public String docType = "<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">\n";
	
	protected final static String ABs = "admin_bdys";
    protected final static String ABIs = "admin_bdys_import";

	public void init() throws ServletException {
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException uhe) {
			System.out.println("Cannot get Server hostname. "+uhe);
			hostname = "___";
		}
		title = "DAB."+hostname.substring(0, 3);
		message = "Admin Boundaries application";
		description = "The downloader interface queries the four destination admin boundary tables comparing them against their temporary source "
				+ "counterparts. Each table-set can be in one of three states, Vacant, Loaded or Transferred. If a table-set is Vacant no temporary "
				+ "tables exist and the import tables must be populated from file.\n"
				+ "Two files are used to populate the admin boundaries; StatsNZ_meshblock_concordance_YYYMMDD.zip and nz_localities.csv. "
				+ "These can be found on the LINZ SFTP and gisdata file share respectively. The StatsNZ meshblock zip file contains two shapefiles and "
				+ "one CSV used to update the tables; meshblock, meshblock_concordance and territorial_authority.\n"
				+ "If a table-set is in the Loaded state the import tables have been built and column changes applied. At this stage concerned users "
				+ "will be notified and if approved changes can be pushed through to the final destiation tables.\n"
				+ "When a table-set is in the Transferred state the import tables will match the destination tables and no action is necessary.\n"
				+ "Actions:\n"
				+ "LOAD - Load import tables from file\n"
				+ "TRANSFER - Transfer import tables to destination tables.\n"
				+ "REJECT - Delete import tables.\n";
	}
	
	
	protected String getHTMLWrapper(String... values){
		StringBuilder sb = new StringBuilder();
		sb.append(docType);
		sb.append("<html>\n");
		for (String s : values){sb.append(s);}
		sb.append("</html>");
		return sb.toString();
	}
	
	protected String getLoading(){
		return String.join("\n" 
				,"<script src=\"http://ajax.googleapis.com/ajax/libs/jquery/1.5.2/jquery.min.js\"></script>"
				,"<script src=\"https://github.com/Modernizr/Modernizr/raw/master/modernizr.js\"></script>"
				,"<script>"
				,"$(window).load(function() {"
				,"$(\"#loader\").animate({"
				,"top: -200"
				,"}, 1500);"
				,"});"
				,"</script>\n");
			
	}
	
	protected String getFavIcon(){return "<link rel=\"icon\" type=\"image/png\" href=\"/ab/linz.dab.png\"/>";}
	protected String getScript(){return "<script src=\"https://code.jquery.com/jquery-3.1.1.js\" type=\"text/javascript\"></script>";}
	protected String getLoaderDiv(){return "<div id=\"loader\"></div>";}
	protected String getLoadScript(){return "<script>$(window).load(function(){$(\"#loader\").fadeOut(\"slow\");});</script>";}
	
	protected String getHead(){
		return String.join("\n"
				,"<head profile=\"http://www.w3.org/2005/10/profile\">"
				,"<meta charset=\"utf-8\"/>" 
				,"<link href=\"main.css\" rel=\"stylesheet\" type=\"text/css\"/>"
				,"<title>",title,"</title>"
				,getScript()
				,getLoadScript()
				,getFavIcon()
				,"</head>");
	}
	
	protected String getBodyHeader(){
		return String.join("\n"
			,"<body>"
			,getLoaderDiv()
			,"<!-- <div id=\"container\"> -->"
			,"<header>"
	    	,"<img src=\"http://www.linz.govt.nz/sites/all/themes/linz_osi/images/logo/logo-linz.png\">\n"
	    	,"</header>\n");
	}
	
	protected String getBodyTitle(){
		return String.join("\n","<h1>",message,"</h1>","<p>",description.replace("\n","</br>\n"),"</p>");
	}
	
	protected String getBodyContent(String... values){
		StringBuilder sb = new StringBuilder();
		for (String s : values){sb.append(s);}
		return sb.toString();
	}
	
	protected String getBodyFooter(Date created, Date accessed, String user){
		return String.join("\n"
				,"<footer><ul>"
				,"<li>Created : ",created.toString(),"</li>"
				,"<li>Accessed : ",accessed.toString(),"</li>"
				,"<li>User : ",user,"</li>"
		    	,"</ul></footer>"
		    	,"<!-- </div> -->"
		    	,"</body>\n");
	}
	
	/**
	 * Redirect post requests to get handler
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
