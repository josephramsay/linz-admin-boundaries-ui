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
	
	public String docType = "<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">\n";
	
	public void init() throws ServletException {
		title = "DAB";
		message = "Downloader for Admin Boundaries";
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
		return String.join("\n"
				,"<h1>"
				,message,
				"</h1>\n");
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
		String li = "";
		for (String k : kv.keySet()){
			li += "<li><b>"+k+"</b> : "+kv.get(k)+"</li>\n";
		}
		return String.join("\n" 
                ,"<ul>\n"
                ,li
                ,"</ul>\n");
	}  
	
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
