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

public class DABServletAction extends DABServlet {
	
	static final long serialVersionUID = 2;
	DABConnector dabc;
	
	public String docType = "<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">\n";
	
	public void init() throws ServletException {
		title = "DABa";
		message = "AIMS Extractor Action";
		dabc = new DABConnector();
	}
	
	/**
	 * Starts a new process controller returning the output from the executed script
	 * @param action User provided 
	 * @return
	 */
	public String readProcessOutput(String action){
		//read admin_bdys diffs
		ProcessControl pc = new ProcessControl();
		return pc.startProcessStage(action);
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		    throws IOException, ServletException {
		
		response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(true);

        Date created = new Date(session.getCreationTime());
        Date accessed = new Date(session.getLastAccessedTime());        
        
        String action = request.getParameter("action").toLowerCase();
        Map<String, String> info = new HashMap<>();
        info.put("ACTION",action);
        info.put("RESULT",readProcessOutput(action));
        
        out.println(docType +
                "<html><head>\n" +
                getHead() +
                "</head>\n<body><div id=\"container\">" +
                getBodyHeader() +
                getBodyTitle() +
                getInfoMessage(info) +
                "</div></body></html>");
        out.println("Created: " + created + "</br>");
        out.println("Last Accessed: " + accessed + "</br>");
	}


}
