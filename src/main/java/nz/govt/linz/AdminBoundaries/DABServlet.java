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

import static nz.govt.linz.AdminBoundaries.DABFormatter.BNAV;

/**
 * Base servlet class
 * @author jramsay
 */
public class DABServlet extends HttpServlet {
	
	static final long serialVersionUID = 1;
	protected String message;
	protected String title;	
	protected String description;	
	public String hostname;
	
	public String docType = "<!DOCTYPE html public \"-//w3c//dtd html 4.0 transitional//en\">\n";
	
	protected final static String ABs = "admin_bdys";
    protected final static String ABIs = "admin_bdys_import";

    /**
     * Servlet initialisation method setting title and message text
     */
	public void init() throws ServletException {
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException uhe) {
			System.out.println("Cannot get Server hostname. "+uhe);
			hostname = "___";
		}
		title = "DAB."+hostname.substring(0, 3);
		message = "Admin Boundaries application";
		description = "This application performs the downloading and importation of admin boundary data needed for AIMS";

	}
	
	/**
	 * Replace parentheses with encoded angle brackets and adds breaks on found new line characters
	 * @param raw
	 * @return
	 */
	protected String encode(String raw){
		return raw.replaceAll("\\(", "&lt;").replaceAll("\\)", "&gt;").replace("\n","<br/>\n");
	}
	
	/**
	 * Wraps varargs content in html tags, fetching content from header and body functions
	 * @param values
	 * @return
	 */
	protected String getHTMLWrapper(String... values){
		StringBuilder sb = new StringBuilder();
		sb.append(docType);
		sb.append("<html>\n");
		sb.append(getHead());
		sb.append(getBodyHeader());
		sb.append(getBodyTitle());
		for (String s : values){sb.append(s);}
		sb.append("</html>");
		return sb.toString();
	}
	
	/**
	 * Returns loading screen animation script text
	 * @return
	 */
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
	
	/**
	 * Returns common header text
	 * @return
	 */
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
	
	/**
	 * Returns common title text used in html body 
	 * @return
	 */
	protected String getBodyHeader(){
		return String.join("\n"
			,"<body>"
			,getLoaderDiv()
			,"<!-- <div id=\"container\"> -->"
			,"<header>"
	    	,"<img src=\"http://www.linz.govt.nz/sites/all/themes/linz_osi/images/logo/logo-linz.png\">\n"
	    	,"</header>\n");
	}
	
	/**
	 * Formats informational title text
	 * @return
	 */
	protected String getBodyTitle(){
		return String.join("\n","<h1>",message,"</h1>","<article class=\"desc\">",encode(description),"</article>\n");
	}
	
	/**
	 * Appends text strings into the content of the html body
	 * @param values
	 * @return
	 */
	protected String getBodyContent(String... values){
		StringBuilder sb = new StringBuilder();
		for (String s : values){sb.append(s);}
		return sb.toString();
	}
	
	/**
	 * Provides informational text in footer of document and additionally provides links to config and summary/main pages
	 * @param created
	 * @param accessed
	 * @param user
	 * @return
	 */
	protected String getBodyFooter(Date created, Date accessed, String user){
		return String.join("\n"
				,"<footer><section class=\"l_foot\"><ul>"
				,"<li>Created : ",created.toString(),"</li>"
				,"<li>Accessed : ",accessed.toString(),"</li>"
				,"<li>User : ",user,"</li>"
		    	,"</ul></section>\n<section class=\"r_foot\">\n"
				,"<a href=\"sum\" class=\""+BNAV+"\">S</a>\n"
				,"<a href=\"cfg\" class=\""+BNAV+"\">C</a>\n"
				,"</section>\n</footer>"
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
