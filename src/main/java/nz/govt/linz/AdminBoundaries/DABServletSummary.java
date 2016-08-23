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

public class DABServletSummary extends DABServlet {
	
	static final long serialVersionUID = 1;
	DABConnector dabc;		
	DABFormatter dabf;
	
	private final static String ABs = "admin_bdys";
    private final static String ABIs = "admin_bdys_import";
	
	public String docType = "<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">\n";
	
	public void init() throws ServletException {
		title = "DABs";
		message = "AIMS Extractor Summary";
		dabc = new DABConnector();
		dabf = new DABFormatter();
	}
	
	
	public void sendNotification(){
		//Send email to users informing them a new import is ready to review 
	}	
	
	public String readChangesetSummary(String schema, String table){
		//read admin_bdys diffs
		String query = String.format("SELECT COUNT(*) count FROM %s.%s",schema,table);
		return dabf.getSummaryAsTable(table,dabc.getExtractSummary(query));
	}	
	
	public String readTableDifferences(String schema, String table){
		//TODO
		String query = String.format("SELECT COUNT(*) count FROM %s.%s",schema,table);
		return dabf.getSummaryAsTable(table,dabc.getExtractSummary(query));
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		    throws IOException, ServletException {
		
		response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(true);

        Date created = new Date(session.getCreationTime());
        Date accessed = new Date(session.getLastAccessedTime());        
        Map<String, String> info = new HashMap<>();
        info.put("USER",request.getParameter("user"));
        
        out.println(docType +
                "<html><head>\n" +
                getHead() +
                "</head>\n<body><div id=\"container\">" +
                getBodyHeader() +
                getBodyTitle() +
                getInfoMessage(info) +
                getSummaryTable() +
                getAcceptDeclineForm() +
                "</div></body></html>");
        out.println("Created: " + created + "</br>");
        out.println("Last Accessed: " + accessed + "</br>");
	}
	
	protected String getAcceptDeclineForm(){
		return String.join("\n"
			,"<form action=\"act\" method=\"GET\">"
			,"<input class=\"formbutton\" type=\"submit\" name=\"action\" value=\"Transfer\">\n"
			,"<input class=\"formbutton\" type=\"submit\" name=\"action\" value=\"Prepare\">\n"
			,"<input class=\"formbutton\" type=\"submit\" name=\"action\" value=\"Reject\"><br/>\n"
			,"</form>");
	}
	
	protected String getSummaryTable(){	    
	    String tsm = readChangesetSummary(ABs,"meshblock");
	    String tmc = readChangesetSummary(ABs,"meshblock_concordance");
	    String tnl = readChangesetSummary(ABs,"nz_locality");
	    String tst = readChangesetSummary(ABs,"territorial_authority");        
	    
	    String tsmi = readChangesetSummary(ABIs,"temp_statsnz_meshblock");
	    String tmci = readChangesetSummary(ABIs,"temp_meshblock_concordance");
	    String tnli = readChangesetSummary(ABIs,"temp_nz_locality");
	    String tsti = readChangesetSummary(ABIs,"temp_statsnz_ta");
		return String.join("\n"
	        ,"<table class=\"result\">"
	        ,"<tr><th>Admin Bdys</th><th>Imported</th></tr>"
	        ,"<tr><td>" + tsm + "</td><td>" + tsmi + "</td></tr>"
	        ,"<tr><td>" + tmc + "</td><td>" + tmci + "</td></tr>"
	        ,"<tr><td>" + tnl + "</td><td>" + tnli + "</td></tr>"
	        ,"<tr><td>" + tst + "</td><td>" + tsti + "</td></tr>"
	        ,"</table>");
	}


}
