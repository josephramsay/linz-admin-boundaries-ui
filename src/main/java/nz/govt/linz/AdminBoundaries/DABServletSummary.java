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
	
	/**
	 * Inner Enumeration matching imported tables to the final refernce tables in the admni_bdys schema
	 * @author jramsay
	 *
	 */
	public enum TableMapping {
		MB("meshblock","temp_statsnz_meshblock","code"),
		MBC("meshblock_concordance","temp_meshblock_concordance","meshblock"),
		NZL("nz_locality","temp_nz_locality","id"),
	    TA("territorial_authority","temp_statsnz_ta","ogc_fid");
		
		private final String dst;
		private final String src;	
		private final String key;	

		private TableMapping(String dst, String src, String key){
				this.dst = dst;
				this.src = src;
				this.key = key;
		}
		public String dst(){return dst;}
		public String src(){return src;}
		public String key(){return key;}
	}

	
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

	
	public String readChangesetSummary(String schema, String table){
		//read admin_bdys diffs
		String query = String.format("SELECT COUNT(*) count FROM %s.%s",schema,table);
		return dabf.getSummaryAsTable(table,dabc.getExtractSummary(query));
	}
	
	/**
	 * Use table_version get_diff func to return differences between to-be merged tables
	 * @param table
	 * @return
	 */
	public String readImportDifference(TableMapping table){
		//read table diffs
		String t1 = String.format("%s.%s", ABs, table.dst());
		String t2 = String.format("%s.%s", ABIs, table.src());
		String query = String.format("SELECT table_version.ver_get_table_differences('%s'.'%s','%s')",t1,t2,table.key());
		return dabf.getSummaryAsTable(table.dst(),dabc.getExtractSummary(query));
	}
	
	
	public String readTableDifferences(String schema, String table){
		//TODO
		String query = String.format("SELECT COUNT(*) count FROM %s.%s",schema,table);
		return dabf.getSummaryAsTable(table,dabc.getExtractSummary(query));
	}
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
		    throws IOException, ServletException {
		String disp = "";
		
		response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(true);

        Date created = new Date(session.getCreationTime());
        Date accessed = new Date(session.getLastAccessedTime());
        
        String action = request.getParameter("compare");
        
        if (action != null) {
        	TableMapping tm = TableMapping.valueOf(action.toUpperCase());
        	disp = readImportDifference(tm);
        }
        else {
        	disp = getSummaryTable();
        }

        
        Map<String, String> info = new HashMap<>();
        info.put("USER",request.getParameter("user"));
        info.put("ACTION",action);
        
        out.println(docType +
                "<html><head>\n" +
                getHead() +
                "</head>\n<body><div id=\"container\">" +
                getBodyHeader() +
                getBodyTitle() +
                getInfoMessage(info) +
                disp +
                getAcceptDeclineForm() +
                "</div></body></html>");
        out.println("Created: " + created + "</br>");
        out.println("Last Accessed: " + accessed + "</br>");
	}
	
	protected String getAcceptDeclineForm(){
		String msg = String.join("<br/>","<ul class=\"styledlist\">"
				,"<li><b>Load.</b> Download new files from SFTP directory</li>"
				,"<li><b>Map.</b> Match columns in import tables to columns in destinations tables (see config file for mappings)</li>"
				,"<li><b>Transfer.</b> Run table_version function to populate destination tables</li>"
				,"<li><b>Reject.</b> Drop the import tables and quit</li>"
				,"</ul>");
		return String.join("\n",msg
			,"<form action=\"act\" method=\"GET\">"
			,"<input class=\"formbutton\" type=\"submit\" name=\"action\" value=\"Load\">\n"
			,"<input class=\"formbutton\" type=\"submit\" name=\"action\" value=\"Map\">\n"
			,"<input class=\"formbutton\" type=\"submit\" name=\"action\" value=\"Transfer\">\n"
			,"<input class=\"formbutton\" type=\"submit\" name=\"action\" value=\"Reject\"><br/>\n"
			,"</form>");
	}
	
	protected String getSummaryTable(){	    
	    String tsm = readChangesetSummary(ABs,TableMapping.MB.dst());
	    String tmc = readChangesetSummary(ABs,TableMapping.MBC.dst());
	    String tnl = readChangesetSummary(ABs,TableMapping.NZL.dst());
	    String tst = readChangesetSummary(ABs,TableMapping.TA.dst());        
	    
	    String tsmi = readChangesetSummary(ABIs,TableMapping.MB.src());
	    String tmci = readChangesetSummary(ABIs,TableMapping.MBC.src());
	    String tnli = readChangesetSummary(ABIs,TableMapping.NZL.src());
	    String tsti = readChangesetSummary(ABIs,TableMapping.TA.src());
	    
		return String.join("\n"
	        ,"<table class=\"result\">"
	        ,"<tr><th>Admin Bdys</th><th>Imported</th></tr>"
	        ,"<tr><td>" + tsm + "</td><td>" + tsmi + "</td></tr>"
	        ,"<tr><td span=2><a href=\"sum?compare=mb\" class=\"button\">Compare MB Tables</a></td></tr>"
	        ,"<tr><td>" + tmc + "</td><td>" + tmci + "</td></tr>"
	        ,"<tr><td span=2><a href=\"sum?compare=mbc\" class=\"button\">Compare MBC Tables</a></td></tr>"
	        ,"<tr><td>" + tnl + "</td><td>" + tnli + "</td></tr>"
	        ,"<tr><td span=2><a href=\"sum?compare=nzl\" class=\"button\">Compare NZL Tables</a></td></tr>"
	        ,"<tr><td>" + tst + "</td><td>" + tsti + "</td></tr>"
	        ,"<tr><td span=2><a href=\"sum?compare=ta\" class=\"button\">Compare TA Tables</a></td></tr>"
	        ,"</table>");
	}


}
