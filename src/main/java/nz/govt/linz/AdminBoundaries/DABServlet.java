package nz.govt.linz.AdminBoundaries;

import java.io.IOException;

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
	
	public String docType = "<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">\n";
	
	public void init() throws ServletException {
		title = "DAB";
		message = "Downloader for Admin Boundaries";
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
	
	protected String getHead(){
		return String.join("\n"
				,"<meta charset=\"utf-8\"/>" 
				,"<link href=\"main.css\" rel=\"stylesheet\" type=\"text/css\"/>"
				,"<title>",title,"</title>"
				,getLoading());
	}
	
	
	protected String getBodyTitle(){
		return String.join("\n","<h1>",message,"</h1>","<p>",description.replace("\n","</br>\n"),"</p>");
	}
	
	protected String getBodyHeader(){
		return String.join("\n"
			,"<header>"
	    	,"<img src=\"http://www.linz.govt.nz/sites/all/themes/linz_osi/images/logo/logo-linz.png\">\n"
	    	,"</header>\n");
	}	
	
	protected String getBodyFooter(Date created, Date accessed, String user){
		return String.join("\n"
				,"<footer><ul>"
				,"<li>Created : ",created.toString(),"</li>"
				,"<li>Accessed : ",accessed.toString(),"</li>"
				,"<li>User : ",user,"</li>"
		    	,"</ul></footer>\n");
		}
	
	
	/**
	 * Formats a set of key/val pairs into an itemised list 
	 * @param kv
	 * @return
	 */
	protected String getInfoMessage(Map<String,String> kv){
		String li_str = "";
		if (kv.size()>0){
			for (String k : kv.keySet()){
				li_str += "<li><b>"+k+"</b> : "+kv.get(k)+"</li>\n";
			}
			return String.join("\n","<article>\n<ul>",li_str,"</ul>\n</article>\n");
		}
		else {
			return "";
		}
	}  
	
	/**
	 * Redirect post requests to get handler
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
