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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ProcessControl {
	
	
	//private static final String PROCESS = "/home/jramsay/workspace/AdminBoundaries/printargs.sh";
	private static final String PROCESS = "/home/lds_bde/scripts/download_admin_bdys.sh";
	
	private ProcessBuilder processbuilder;

	final static String FILENAME = "postgresql.properties";//config.txt"
	
	/**
	 * Initialises ProcessBuilder returning output from requested script 
	 * @param stage Argument to the aims_extract.sh script indicating transfer, process or reject
	 * @return
	 */
	public String startProcessStage(String stage){
		StringBuilder sb = new StringBuilder();
		processbuilder = new ProcessBuilder("/bin/bash",PROCESS,stage);
		try {
			Process process = processbuilder.start();
			final InputStream is = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			String line;
			while (process.isAlive()){
				line = reader.readLine();
				if (line != null) {
					sb.append(line+"</br>\n");
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sb.toString();
	}
	
	public String toString(){
		return "ProcessControl::"+processbuilder;
	}
	
	public static void main(String[] args){
		ProcessControl pc = new ProcessControl();
		pc.startProcessStage("transfer");

	}
	
}
