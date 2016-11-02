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

import nz.govt.linz.AdminBoundaries.DABContainerComp.TableInfo;
import nz.govt.linz.AdminBoundaries.DABContainerComp.ImportStatus;

/**
 * Servlet to allow user to edit python dab config file without redeploying
 * @author jramsay
 *
 */
public class DABServletConfigEdit extends DABServlet {

	
	static final long serialVersionUID = 1;
	
	/** Formatter class for converting data-maps to html strings */
	private DABFormatter dabf;
	
	private DABIniReader reader;

	public String docType = "<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">\n";
	
	/**
	 * Initialise servlet class setting status and getting formatter + connector instances
	 */
	public void init() throws ServletException {
		super.init();
		message = "Config Editor for Admin Boundarys";
		dabf = new DABFormatter();
		try {
			reader = new DABIniReader();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	

	/**
	 * Servlet doGet
	 * @param request
	 * @param response
	 */
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		    throws IOException, ServletException {
		String summarytable = "";
		String accdectable = "";
		String infomessage = "";
		String sp = request.getServletPath();//  getServletContext().getRealPath("/");
		
		response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(true);

        Date created = new Date(session.getCreationTime());
        Date accessed = new Date(session.getLastAccessedTime());
        String user = (String) request.getAttribute("currentSessionUser");

        //String user = request.getParameter("user");
        String compare = request.getParameter("compare");
        String action = request.getParameter("action");
        
        /* If compare action requested for table generate a diff table.
         * If action requested start processcontrol and return result.
         * Otherwise return the standard summary table
         */
        
        Map<String, String> info = new HashMap<>(); 
        
        if (action != null) {
        	//s = summary, a = 1
            info.put("ACTION",action);
            info.put("RESULT",action);
        }
        else {
         
        }
        
        infomessage = dabf.getInfoMessage(info);
        
        //OUTPUT
        
        out.println(docType +
                "<html>\n<head>\n" +
                getHead() +
                "</head>\n<body>\n" +
                getBodyHeader() +
                getBodyTitle() +
                getBodyContent(infomessage,summarytable,accdectable) +
                getBodyFooter(created,accessed,user) +
                "</body>\n</html>");

	}


}
