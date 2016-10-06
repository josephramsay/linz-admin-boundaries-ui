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
	private static String DEF_TABLE = "<table><caption>no table</caption>"
			+"<thead><tr><th><i>dabs</i></th></tr></thead>"
			+"<tbody><tr><td><i>data unavailable</i></td></tr></tbody></table>";
	
	/**
	 * Enum representing stages of import
	 * @author jramsay
	 */
	public enum ImportStatus {BLANK,LOADED,COMPLETE}
	
	/**
	 * Inner Enumeration matching imported tables to the final reference tables in the admni_bdys schema
	 * @author jramsay
	 */
	public enum TableMapping {
		MB("meshblock","statsnz_meshblock","code"),
		MBC("meshblock_concordance","meshblock_concordance","meshblock"),
		NZL("nz_locality","nz_locality","id"),
	    TA("territorial_authority","statsnz_ta","ogc_fid");
		
		private final String dst;
		private final String tmp;		
		private final String key;	

		private TableMapping(String dst, String tmp, String key){
				this.dst = dst;
				this.tmp = "temp_"+tmp;
				this.key = key;
		}
		public String dst(){return dst;}
		public String tmp(){return tmp;}
		public String key(){return key;}
		//Get status message for this table
		public String ttl(ImportStatus status){
			switch (status) {
				case BLANK: return "Import: "+tmp+"("+key+") &larr; SFTP";
				case LOADED: return "Transfer: "+dst+"("+key+") &larr; "+tmp+"("+key+")";
				case COMPLETE: return "Complete: "+dst+"("+key+") == "+tmp+"("+key+")";
				default: return "Load: "+dst+" Unavailable";
			}
		}
		//get table name to display on right of summary
		public String dsp(ImportStatus status){
			switch (status) {
				case BLANK: return DEF_TABLE;
				case LOADED: return tmp();
				case COMPLETE: return tmp();
				default: return "";
			} 

		}
	}

	
	static final long serialVersionUID = 1;
	DABConnector dabc;		
	DABFormatter dabf;
	//ImportStatus status = ImportStatus.BLANK;
	
	Map<TableMapping,ImportStatus> status = new HashMap<>();
	ImportStatus lowstatus = ImportStatus.BLANK;
	
	private final static String ABs = "admin_bdys";
    private final static String ABIs = "admin_bdys_import";

	public String docType = "<!doctype html public \"-//w3c//dtd html 4.0 transitional//en\">\n";
	
	/**
	 * Initialise servlet class setting status and getting formatter + connector instances
	 */
	public void init() throws ServletException {
		super.init();
		title = "DAB";
		message = "Downloader for Admin Boundarys";
		dabc = new DABConnector();
		dabf = new DABFormatter();
		initStatus();

	}
	
	/**
	 * Initialises the status array reading table availability
	 */
	private void initStatus(){
		for (TableMapping tm : TableMapping.values()){
			status.put(tm, getStatus(tm));
		}
		lowstatus = status.values().stream().sorted().findFirst().get();
	}

	/**
	 * Determine state of database by testing for tables temp_X, snap_X and dst=snap
	 * @return ImportStatus for selected table
	 * TODO rewrite test for mapped status reflecting geo ops instead of snap build 
	 */
	public ImportStatus getStatus(TableMapping tm){
		//check that imported temp files exist
		String exist_query = String.format("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='%s' AND table_name='%s')",ABIs,tm.tmp());
		//System.out.println("1TQ "+exist_query+" / "+dabc.executeTFQuery(exist_query));
		if (dabc.executeTFQuery(exist_query)){
			//get original column names (so column order isn't considered in comparison
			String col_query = String.format("select array_to_string(array_agg(column_name::text),',') " +
					"from information_schema.columns " +
					"where table_schema='%s' " +
					"and table_name='%s'", ABs, tm.dst());
			String columns = dabc.executeSTRQuery(col_query);
			//System.out.println("2CQ "+col_query+" / "+columns);
			//tmp files match dst files
			String tt = String.format("SELECT %s FROM %s.%s", columns, ABIs, tm.tmp());
			String dt = String.format("SELECT %s FROM %s.%s", columns, ABs,  tm.dst());
			String cmp_query = String.format("SELECT NOT EXISTS (%s EXCEPT %s UNION %s EXCEPT %s)",tt,dt,dt,tt);
			//System.out.println("3CQ "+cmp_query+" / "+dabc.executeTFQuery(cmp_query));
			if (dabc.executeTFQuery(cmp_query)){
				return ImportStatus.COMPLETE;
			}
			return ImportStatus.LOADED;
		}
		return ImportStatus.BLANK;
	}
	
	
	
	/**
	 * Return htmltable containing table row count 
	 * @param schema
	 * @param table
	 * @return
	 */
	public String readChangesetSummary(String schema, String table){
		//read admin_bdys diffs
		if (table == null){
			return DEF_TABLE;
		}
		else {
			String query = String.format("SELECT COUNT(*) count FROM %s.%s",schema,table);
			return dabf.getSummaryAsTable(table,dabc.getExtractSummary(query));
		}
	}
	
	/**
	 * Use table_version get_diff func to return differences between the temp and destination tables
	 * @param table
	 * @return
	 */
	public String readImportDifference(TableMapping table){
		//read table diffs
		String t1 = String.format("%s.%s", ABs, table.dst());
		String t2 = String.format("%s.%s", ABIs, table.tmp());
		String query = String.format("SELECT table_version.ver_get_table_differences('%s','%s','%s')",t1,t2,table.key());
		return dabf.getSummaryAsTable(table.dst(),dabc.getExtractSummary(query));
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
		String summarytable = "";
		String accdectable = "";
		String infomessage = "";
		String sp = request.getServletPath();//  getServletContext().getRealPath("/");
		
		response.setContentType("text/html; charset=UTF-8");
        PrintWriter out = response.getWriter();
        
        HttpSession session = request.getSession(true);

        Date created = new Date(session.getCreationTime());
        Date accessed = new Date(session.getLastAccessedTime());
        
        String user = request.getParameter("user");
        String compare = request.getParameter("compare");
        String action = request.getParameter("action");
        
        /* If compare action requested for table generate a diff table.
         * If action requested start processcontrol and return result.
         * Otherwise return the standard summary table
         */
        
        Map<String, String> info = new HashMap<>();
        
        switch (lowstatus){
        case BLANK: 
        	//show dst table  - <blank>
        	summarytable = DEF_TABLE;
        case LOADED:
        	//show tab;e diffs
        	summarytable = getFullSummary();        	
        	//summarytable = readImportDifference(tm);
        case COMPLETE:
        	//show counts match
        	summarytable = getFullSummary();
        default:
        }

        
        
        if (compare != null) {
        	//s = per table compare, a = 1
        	TableMapping tm = TableMapping.valueOf(compare.toUpperCase());
        	info.put("COMPARE",compare);
        	info.put("RESULT",tm.dst()+" &larr; "+tm.tmp());
        }
        else if (action != null) {
        	//s = summary, a = 1
            info.put("ACTION",action);
            info.put("RESULT",readProcessOutput(action));
         
        }
        
        infomessage = getInfoMessage(info);
        	
        accdectable = dabf.getAcceptDeclineNav(lowstatus.ordinal());
        
        //OUTPUT
        
        out.println(docType +
                "<html>\n<head>\n" +
                getHead() +
                "</head>\n<body>\n" +
                //"<img src=\"loading.gif\" id=\"loader\" align=\"middle\" height=\"100\" width=\"100\"/>" +
                "<div id=\"container\">\n" +
                getBodyHeader() +
                getBodyTitle() +
                infomessage +
                summarytable +
                accdectable +
                getBodyFooter(created,accessed,user) +
                "</div>\n</body>\n</html>");


	}
	protected String getFullSummary(){
		return String.join(""
				,getSummarySection(TableMapping.MB)
				,getSummarySection(TableMapping.MBC)
				,getSummarySection(TableMapping.NZL)
				,getSummarySection(TableMapping.TA)
			);
	}
	
	protected String getSummarySection(TableMapping tablemapping){
		ImportStatus is = status.get(tablemapping);
		String b_colour = "b_green";
		if (is == ImportStatus.BLANK){
			b_colour = "b_red";
		}
		String detail = String.join("\n"
				,"<section class=\"detail\">"
				,"<p><a href=\"sum?compare="+tablemapping.toString()+"\" class=\""+b_colour+"\">Compare "+tablemapping.toString()+" Tables</a>"
				,tablemapping.ttl(is)+"</p>"
				,"</section>\n");
	    String left = String.join("\n"
	    		,"<section class=\"box\">"
	    		,readChangesetSummary(ABs,tablemapping.dst())
	    		,"</section>\n");
	    String right = String.join("\n"
	    	    ,"<section class=\"box\">"
	    	    ,is == ImportStatus.BLANK ? tablemapping.dsp(is) : readChangesetSummary(ABIs,tablemapping.dsp(is))
	    	    ,"</section>\n");
	    
	    return "<article>\n" + left + right + detail + "</article>\n";
	    
	}


}
