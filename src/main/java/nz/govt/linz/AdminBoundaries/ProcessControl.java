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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

/**
 * Class to initiate and run system processes, n this case the included python script, linz_admin_boundaries_uploader.py
 * @author jramsay
 *
 */
public class ProcessControl {
	
	private static final Logger LOGGER = Logger.getLogger( ProcessControl.class.getName() );
	
	//this is the path where the debian packager puts the py part of the app
	private final static String DABP = "webapps/ab/WEB-INF/scripts/linz_admin_boundaries_uploader.py";
	private static final String DELIM = "<br/>\n";
	private static Shell shell;
	
	private enum Shell { Python, Bash;
		private static Shell inspect(String pn) {
			switch (pn.substring(pn.length() - 2).toLowerCase()) {
			case "py": return Python;
			case "sh": return Bash;
			default: return null;
			}
		}
	}

	private static String processname;
	
	/**
	 * Null constructor using default config file location  
	 */
	public ProcessControl(){
		this(DABP);
	}
	
	/**
	 * Constructor sets up config file path
	 * @param procarg
	 */
	public ProcessControl(String child){
		//File parent = new File( System.getProperty( "catalina.base" ) ).getAbsoluteFile();
		this( new File(System.getProperty( "catalina.base" ), child) );

	}
	
	/**
	 * Constructor reads config file and tests accessibility
	 * @param procfile
	 */
	public ProcessControl(File procfile){
		if (procfile.canRead()) { processname = procfile.toString(); }
		else { processname = DABP; }
		shell = Shell.inspect(processname);
		System.setSecurityManager(null);
	}
	
	
	public static ProcessBuilder getProcessBuilder(String arg) {
		ProcessBuilder processbuilder = new ProcessBuilder(shell.name().toLowerCase(),processname,arg);
		processbuilder.redirectErrorStream(true);
		return processbuilder;
	}
	/**
	 * Initialises ProcessBuilder returning output from requested script 
	 * @param arg Argument to the aims_extract.sh script indicating transfer, process or reject
	 * @return
	 */
	public String runProcess(String arg){ 
		LOGGER.finer("process out "+arg);
		StringBuilder sb1 = new StringBuilder();
		sb1.append(arg+DELIM);
		sb1.append(readProcessOutput(getProcessBuilder(arg),DELIM));
		return sb1.toString();
	}
	
	public String readProcessOutput(ProcessBuilder pb, String delimiter) {
		StringBuffer sb2 = new StringBuffer();			
		String line;
		try {
			Process process = pb.start();
			final InputStream is = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));		
			do {
				while ((line = reader.readLine()) != null) { sb2.append(line+delimiter); }
			} 
			while (process.isAlive());
			LOGGER.finer("process out "+sb2.toString());
			if (sb2.length()==0){
				sb2.append("No return value. Process exit_code="+process.exitValue()+delimiter);
			}
		}
		catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		}
		return sb2.toString();
		
	}
	
	public String toString(){
		return "ProcessControl::" + processname;
	}
	
}

