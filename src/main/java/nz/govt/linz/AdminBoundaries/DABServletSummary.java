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

import nz.govt.linz.AdminBoundaries.TableMapList.TableMap;
import nz.govt.linz.AdminBoundaries.TableMapList.ImportStatus;


public class DABServletSummary extends DABServlet {

	
	static final long serialVersionUID = 1;
	DABConnector dabc;		
	DABFormatter dabf;
	TableMapList tml;
	
	Map<TableMap,ImportStatus> status = new HashMap<>();
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
		tml = new TableMapList();
		initStatus();

	}
	
	/**
	 * Initialises the status array reading table availability
	 */
	private void initStatus(){
		for (TableMap tm : tml.values()){
			status.put(tm, getStatus(tm));
		}
		lowstatus = status.values().stream().sorted().findFirst().get();
	}

	/**
	 * Determine state of database by testing for tables temp_X, snap_X and dst=snap
	 * @return ImportStatus for selected table
	 * TODO rewrite test for mapped status reflecting geo ops instead of snap build 
	 */
	public ImportStatus getStatus(TableMap tm){
		//check that imported temp files exist
		String exist_query = String.format("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='%s' AND table_name='%s')",ABIs,tm.tmp());
		//System.out.println("1TQ "+exist_query+" / "+dabc.executeTFQuery(exist_query));
		if (dabc.executeTFQuery(exist_query)){
			//get original column names (so column order isn't considered in comparison
			String col_query = String.format("select array_to_string(array_agg(column_name::text),',') " +
					"from information_schema.columns " +
					"where table_schema='%s' " +
					"and table_name='%s'", ABs, tm.dst());
			String columns = quoteSpace(dabc.executeSTRQuery(col_query));
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
	 * Double quotes column names with spaces in them
	 * @param columns
	 * @return
	 */
	private String quoteSpace(String columns){
		StringBuilder res = new StringBuilder(); 
		for (String col : columns.split(",")){
		    if (col.trim().indexOf(" ")>0){
		        res.append("\""+col+"\"");
		    } 
		    else {
		        res.append(col);
		    }
		    res.append(",");
		}
		return res.deleteCharAt(res.lastIndexOf(",")).toString();
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
			return tml.DEF_TABLE;
		}
		else {
			String query = String.format("SELECT COUNT(*) count FROM %s.%s",schema,table);
			return dabf.getSummaryAsTable(table,dabc.executeQuery(query));
		}
	}
	
	/**
	 * Use table_version get_diff func to return differences between the temp and destination tables
	 * @param tm
	 * @return
	 */
	public String compareTables(TableMap tm){
		//read table diffs
		String t1 = String.format("%s.%s", ABs, tm.dst());
		String t2 = String.format("%s.%s", ABIs, tm.tmp());
		String rec = String.format("T(code char(1), id %s)",colType(ABs+"."+tm.dst(),tm.key()));
		String query = String.format("SELECT T.id, T.code FROM table_version.ver_get_table_differences('%s','%s','%s') as %s",t1,t2,tm.key(),rec);
		return "<article>" + dabf.getSummaryAsTable(tm.dst(),dabc.executeQuery(query)) + "</article>";
	}
	
	private String colType(String tablename, String colname){
		String query = String.format("SELECT table_version.ver_table_key_datatype('%s','%s')",tablename,colname);
		return dabc.executeSTRQuery(query);
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
        
        if (compare != null) {
        	//s = per table compare, a = 1
        	TableMap tm = tml.keyOf(compare.toUpperCase());
        	info.put("COMPARE",compare);
        	info.put("RESULT",tm.dst()+" &larr; "+tm.tmp());
        	summarytable = compareTables(tm);
        	accdectable = dabf.getAlternateNav();
        }
        else if (action != null) {
        	//s = summary, a = 1
            info.put("ACTION",action);
            info.put("RESULT",readProcessOutput(action));
            summarytable = getFullSummary();
            accdectable = dabf.getAcceptDeclineNav(lowstatus.ordinal());
        }
        else {
            switch (lowstatus){
            case BLANK: 
            	//show dst table  - <blank>
            	summarytable = tml.DEF_TABLE;
            case LOADED:
            case COMPLETE:
            	//show counts match
            	summarytable = getFullSummary();
            default:
            }
            accdectable = dabf.getAcceptDeclineNav(lowstatus.ordinal());
         
        }
        
        infomessage = getInfoMessage(info);
        
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
	
	private String getFullSummary(){
		String res = "";
		for (String tm_str : tml.TMM.keySet()){
			res = res.concat(getSummarySection(tml.valueOf(tm_str)));
		}
		return res;
		
	}
	
	/**
	 * Builds table comparison section for a particular tablemapping type
	 * @param tm
	 * @return
	 */
	private String getSummarySection(TableMap tm){
		ImportStatus is = status.get(tm);
		String b_colour = "b_green";
		if (is == ImportStatus.BLANK){
			b_colour = "b_red";
		}
		System.out.println(tm);
		String detail = String.join("\n"
				,"<section class=\"detail\">"
				,"<p><a href=\"sum?compare="+tm.abv()+"\" class=\""+b_colour+"\">Compare "+tm.abv().toUpperCase()+" Tables</a>"
				,tm.ttl(is)+"</p>"
				,"</section>\n");
	    String left = String.join("\n"
	    		,"<section class=\"box\">"
	    		,readChangesetSummary(ABs,tm.dst())
	    		,"</section>\n");
	    String right = String.join("\n"
	    	    ,"<section class=\"box\">"
	    	    ,is == ImportStatus.BLANK ? tm.dsp(is) : readChangesetSummary(ABIs,tm.dsp(is))
	    	    ,"</section>\n");
	    
	    return "<article>\n" + left + right + detail + "</article>\n";
	    
	}


}
