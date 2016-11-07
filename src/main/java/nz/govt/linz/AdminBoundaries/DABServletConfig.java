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
		String sp = request.getServletPath();//  getServletContext().getRealPath("/");
		
		response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(true);

        Date created = new Date(session.getCreationTime());
        Date accessed = new Date(session.getLastAccessedTime());
        String user = (String) request.getAttribute("currentSessionUser");
        
        /** check parameters list, if anything parse, store and redisplay */
        Map<String, Map<String,String>> config = new LinkedHashMap<>();
        Enumeration<String> params = request.getParameterNames(); 
        while (params.hasMoreElements()) {
        	String pname = params.nextElement();
        	String[] parts = pname.split("_");
        	if (config.containsKey(parts[0])) {
        		config.get(parts[0]).put(parts[1], request.getParameterValues(pname)[0]);
        	}
        	else {
        		config.put(parts[0], new LinkedHashMap<String,String>(){{put(parts[1],request.getParameterValues(pname)[0]);}});
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
