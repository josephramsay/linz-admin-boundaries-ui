package nz.govt.linz.AdminBoundaries;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;

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
import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.http.*;

import static nz.govt.linz.AdminBoundaries.DABFormatter.BNAV;

/**
 * Base servlet class
 * @author jramsay
 */
public class DABServlet extends HttpServlet {
	
	private static final Logger LOGGER = Logger.getLogger(DABServlet.class.getName());
	
	static final long serialVersionUID = 100L;
	protected String message;
	protected String title;	
	protected String description;	
	public String hostname;
	
	public String docType = "<!DOCTYPE html public \"-//w3c//dtd html 4.0 transitional//en\">\n";
	
	//private final static String CONF_PATH = "WEB-INF/scripts/download_admin_bdys.ini";
	private final static String CONF_NAME = "linz_admin_boundaries_uploader.ini";
	private final static String CONF_PATH = "WEB-INF/scripts/conf/";
	
	//TODO use config set schema vars
	private final static String ABs_def = "admin_bdys";
    private final static String ABIs_def = "admin_bdys_import";	
    protected static String ABs;
    protected static String ABIs;
    
	/** Formatter class for converting data-maps to html strings */
	protected DABFormatter dabf;
	/** Class holding info on tables for comparson */
	protected DABContainerComp ccomp;
	//** Config reader */
	protected DABIniReader reader;

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
		description = "This application performs the download and import of admin boundary data needed for AIMS";
		
		dabf = new DABFormatter();
		try {
			reader = new DABIniReader(getServletContext().getRealPath(CONF_PATH+CONF_NAME));
		}
		catch (NullPointerException npe) {
			LOGGER.warning("Error reading config, "+npe);
			String fps = Paths.get("").toAbsolutePath().toString();
			String rps = "/src/main/webapp/";
			reader = new DABIniReader(fps+rps+CONF_PATH+CONF_NAME);
		}
		ccomp = new DABContainerComp(reader);
		
		ABs = reader.get("database", "originschema", ABs_def);
		ABIs = reader.get("database", "schema", ABIs_def);

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
	
	protected String getSpinner() {return "<script>spin.js</script>";}
	protected String activateSpinner() {return "import {Spinner} from 'spin.js';\n" + 
			"\n" + 
			"var opts = {\n" + 
			"  lines: 13, // The number of lines to draw\n" + 
			"  length: 38, // The length of each line\n" + 
			"  width: 17, // The line thickness\n" + 
			"  radius: 45, // The radius of the inner circle\n" + 
			"  scale: 1, // Scales overall size of the spinner\n" + 
			"  corners: 1, // Corner roundness (0..1)\n" + 
			"  color: '#ffffff', // CSS color or array of colors\n" + 
			"  fadeColor: 'transparent', // CSS color or array of colors\n" + 
			"  opacity: 0.25, // Opacity of the lines\n" + 
			"  rotate: 0, // The rotation offset\n" + 
			"  direction: 1, // 1: clockwise, -1: counterclockwise\n" + 
			"  speed: 1, // Rounds per second\n" + 
			"  trail: 60, // Afterglow percentage\n" + 
			"  fps: 20, // Frames per second when using setTimeout() as a fallback in IE 9\n" + 
			"  zIndex: 2e9, // The z-index (defaults to 2000000000)\n" + 
			"  className: 'spinner', // The CSS class to assign to the spinner\n" + 
			"  top: '50%', // Top position relative to parent\n" + 
			"  left: '50%', // Left position relative to parent\n" + 
			"  shadow: none, // Box-shadow for the lines\n" + 
			"  position: 'absolute' // Element positioning\n" + 
			"};\n" + 
			"\n" + 
			"var target = document.getElementById('spintarget');\n" + 
			"var spinner = new Spinner(opts).spin(target);";
	}
	
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
				,getSpinner()
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
