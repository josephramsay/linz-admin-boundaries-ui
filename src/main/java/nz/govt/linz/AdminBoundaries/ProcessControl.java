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

/**
 * Class to initiate and run system processes, n this case the included python script, download_admin_bdys.py
 * @author jramsay
 *
 */
public class ProcessControl {
	
	//this is the path where the debian packager puts the py part of the app
	//private final static String DABP = "/usr/local/share/AdminBoundaries/download_admin_bdys.sh";
	private final static String DABP = "webapps/ab/WEB-INF/scripts/download_admin_bdys.py";
	private final static String SHELL = "python";
	
	private ProcessBuilder processbuilder;
	private String processname;
	
	public ProcessControl(){
		this(DABP);
	}
	public ProcessControl(String procarg){
		File catalina_base = new File( System.getProperty( "catalina.base" ) ).getAbsoluteFile();
		File proc_file = new File(catalina_base, procarg);
		if (proc_file.canRead()) {
			processname = proc_file.toString();
		}
		else {
			processname = DABP;
		}
		System.setSecurityManager(null);
	}
	
	/**
	 * Initialises ProcessBuilder returning output from requested script 
	 * @param arg Argument to the aims_extract.sh script indicating transfer, process or reject
	 * @return
	 */
	public String startProcessStage(String arg){ 
		StringBuilder sb = new StringBuilder();
		sb.append(arg+"<br/>\n");
		processbuilder = new ProcessBuilder(SHELL,processname,arg);
		processbuilder.redirectErrorStream(true);
		try {
			Process process = processbuilder.start();
			final InputStream is = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line;
			while (process.isAlive()){
				while ((line = reader.readLine()) != null) {
					sb.append(line+"<br/>\n");
				}
			}
			if (sb.length()==0){
				sb.append("No return value. Process exit_code="+process.exitValue()+"<br/>\n");
			}
		} 
		catch (IOException ioe) {
			// TODO Auto-generated catch block
			sb.append("ProcessBuilder IO Failure. "+ioe.toString()+"<br/>\n");
		} 
		return sb.toString();
	}
	
	public String toString(){
		return "ProcessControl::" + processbuilder;
	}
	
	public static void main(String[] args){
		ProcessControl pc = new ProcessControl("/home/<user>/git/AdminBoundaries/scripts/download_admin_bdys.sh");
		System.out.println( pc.startProcessStage("load") );
		System.out.println( pc.startProcessStage("map") );
		System.out.println( pc.startProcessStage("transfer") );

	}
	
}

