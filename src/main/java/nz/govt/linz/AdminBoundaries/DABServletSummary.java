package nz.govt.linz.AdminBoundaries;

import static nz.govt.linz.AdminBoundaries.DABFormatter.BGRN;
import static nz.govt.linz.AdminBoundaries.DABFormatter.BRED;

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
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nz.govt.linz.AdminBoundaries.DABContainerComp.ImportStatus;
import nz.govt.linz.AdminBoundaries.DABContainerComp.TableInfo;

/**
 * Main servlet page displaying table import status and possible actions
 * @author jramsay
 *
 */
public class DABServletSummary extends DABServlet {

	private static final Logger LOGGER = Logger.getLogger( DABServletSummary.class.getName() );
	
	static final long serialVersionUID = 110L;
	
	/** Database connector and query wrapper */
	private DABConnector dabc;		

	
	/** Map of the status for each table pair */ 
	private Map<TableInfo,ImportStatus> status = new HashMap<>();
	
	/** Lowest status value across tables for button colouring*/
	private ImportStatus lowstatus = ImportStatus.BLANK;
	
	/**
	 * Initialise servlet class setting status and getting formatter + connector instances
	 */
	public void init() throws ServletException {
		super.init();
		message = "Downloader for Admin Boundarys";		
		description = String.join("\n", 
				"The downloader interface queries the four destination admin boundary tables comparing them against their temporary source "
				+ "counterparts. Each table-set can be in one of three states, Vacant, Loaded or Transferred. If a table-set is Vacant no temporary "
				+ "tables exist and the import tables must be populated from file/WFS.",
				"The admin boundaries tables are populated over WFS from the StatsNZ data service and from a locally saved file; nz_localities.csv. "
				+ "The StatsNZ dataservice provides; meshblock, meshblock_concordance and territorial_authority.",
				"If a table-set is in the Loaded state the import tables have been built and column changes applied. At this stage selected users "
				+ "will be notified and if approved, changes can be pushed through to the final destination tables.",
				"When a table-set is in the Transferred state the import tables will match the destination tables and no action is necessary.",
				"In the summary screen, tables are compared using row counts. For a more detailed table comparison use the 'Compare ### Table' button "
				+ "which returns the results from the table_version function get_table_differences() indicating row number and proposed operation "
				+ "(u)pdate, (a)dd or (d)elete",
				"<br/><b>Actions</b>",
				"<br/><u>LOAD</u> :: Load import tables from file",
				"<br/><u>TRANSFER</u> :: Transfer import tables to destination tables.",
				"<br/><u>REJECT</u> :: Delete import tables.",
				"<br/><u>OPTIONAL</u> :: Run any configured post-processing functions.");
		dabc = new DABConnector();
		//updateStatus();
	}
	
	/**
	 * Initialises the status array, reading table availability and calculating least ready state 
	 */
	private void updateStatus(){
		for (TableInfo ti : ccomp.values()){
			LOGGER.fine("Getting status for TI, "+ti);
			status.put(ti, dabc.getStatus(ti));
		}
		lowstatus = status.values().stream().sorted().findFirst().get();
	}

	
	/**
	 * Starts a new process controller returning the output from the executed script
	 * @param action User provided 
	 * @return
	 */
	public String readProcessOutput(String action){
		//read admin_bdys diffs
		ProcessControl pc = new ProcessControl();
		return pc.runProcess(action);
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
		//String sp = request.getServletPath();//  getServletContext().getRealPath("/");
		
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
        
        if (compare != null) {
        	//s = per table compare, a = 1
        	TableInfo ti = ccomp.keyOf(compare.toUpperCase());
        	info.put("COMPARE",compare);
        	info.put("RESULT",ti.dst()+" &larr; "+ti.tmp());
        	//String t = String.format("DST %s, KEY %s, TYP %s", ti.dst(), ti.key(), dabc.colType(ABs + "." +ti.dst(), ti.key()));
        	//info.put("DST KEY",t);
        	summarytable = dabc.compareTableData(ti);
        	accdectable = dabf.getBackNav();
        }
        else if (action != null) {
        	//s = summary, a = 1
            info.put("ACTION",action);
            info.put("RESULT",readProcessOutput(action));
            updateStatus();
            summarytable = getFullSummary();
            accdectable = dabf.getNavigation(lowstatus.ordinal());
        }
        else {
        	updateStatus();
            switch (lowstatus){
            case BLANK: 
            	//show dst table  - <blank>
            	summarytable = DABContainerComp.DEF_TABLE;
            case LOADED:
            case COMPLETE:
            	//show counts match
            	summarytable = getFullSummary();
            default:
            }
            accdectable = dabf.getNavigation(lowstatus.ordinal());
        }
        
        infomessage = dabf.getInfoMessage(info);
        
        //OUTPUT
        out.println(getHTMLWrapper(
                getBodyContent(infomessage,summarytable,accdectable),
                getBodyFooter(created,accessed,user)
                )
        	);
	}
	
	/**
	 * Get all table side-by-side comparison articles
	 * @return
	 */
	private String getFullSummary(){
		String res = "";
		for (String tm_str : DABContainerComp.TABV.keySet()){
			res = res.concat(getSummarySection(ccomp.valueOf(tm_str)));
		}
		return res;
		
	}
	
	/**
	 * Builds table comparison article for a particular tableinfo type
	 * @param ti
	 * @return
	 */
	private String getSummarySection(TableInfo ti){
		ImportStatus is = status.get(ti);
		String b_col,href;
		if (is == ImportStatus.BLANK){
			b_col = BRED;
			href = "/ab";
		}
		else {
			b_col = BGRN;
			href = "sum?compare="+ti.abv();
		}
		String detail = String.join("\n"
				,"<section class=\"detail\">"
				,"<p><a href=\"" + href + "\" class=\""+b_col+"\">Compare "+ti.abv().toUpperCase()+" Tables</a>"
				,ti.ttl(is)+"</p>"
				,"</section>\n");
	    String left = String.join("\n"
	    		,"<section class=\"box\">"
	    		,dabc.compareTableCount(ABs,ti.dst())
	    		,"</section>\n");
	    String right = String.join("\n"
	    	    ,"<section class=\"box\">"
	    	    ,is == ImportStatus.BLANK ? ti.dsp(is) : dabc.compareTableCount(ABIs,ti.dsp(is))
	    	    ,"</section>\n");
	    
	    return "<article>\n" + left + right + detail + "</article>\n";
	    
	}

	/**
	 * main method used for testing
	 * 
	 * @param args
	 * @throws ServletException 
	 */
	public static void main(String[] args) throws ServletException {
		DABServletSummary dabss = new DABServletSummary();
		dabss.init();
		dabss.getFullSummary();
	}
}
