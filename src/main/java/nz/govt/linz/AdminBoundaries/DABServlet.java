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
		title = "DABs";
		message = "Downloader for Admin Boundaries";
	}
	
	protected String getHead(){
		return String.join("\n"
				,"<meta charset=\"utf-8\"/>\n" 
				,"<link href=\"main.css\" rel=\"stylesheet\" type=\"text/css\"/>\n"
				,"<title>",title,"</title>\n");
	}
	
	
	protected String getBodyTitle(){
		return String.join("\n"
				,"<h1 align=\"center\">"
				,message,
				"</h1>\n");
	}
	
	protected String getBodyHeader(){
		return String.join("\n"
			,"<header>"
			,"<div class=\"width\">"
			,"<h1><a href=\"/\">LI<span>NZ</span></a></h1>"
	    	,"<h2>land information new zealand</h2>"
	    	,"</div>"
	    	,"</header>");
	}
	
	protected String getInfoMessage(Map<String,String> kv){
		String li = "";
		for (String k : kv.keySet()){
			li += "<li><b>"+k+"</b>: "+kv.get(k)+"</li>\n";
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
