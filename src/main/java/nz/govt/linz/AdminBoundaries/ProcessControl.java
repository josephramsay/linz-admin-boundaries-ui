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
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;
import java.util.logging.Level;

import py4j.Gateway;
import py4j.GatewayServer;
import py4j.Py4JException;
import py4j.Py4JServerConnection;
import py4j.commands.Command;

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
	
	public interface GatewayLoggerInterface {
		void critical(String source);
		void error(String source);
		void warning(String source);
		void info(String source);
		void debug(String source);
	}
	
	/**
	 * Inner class wrapping GatewayLogger|Server as autocloseable
	 * @author jramsay
	 *
	 */
	private class GatewayWrapper implements AutoCloseable {
		
		private final int GWPORT = 25300+(int)(new Random()).nextInt(50);
		GatewayServer gwserver;

		private GatewayWrapper(Logger _logger) {
			gwserver = new GatewayServer(new CommandEntryPoint(_logger));//,GWPORT);
			LOGGER.info("JavaGateway startup1");
			gwserver.start();
			LOGGER.info("JavaGateway startup2");
		}
		
		@Override
		public void close() throws Exception {
			LOGGER.info("JavaGateway shutdown");
			gwserver.shutdown();
		}
	}
	
	//--------------------------------------------------------------------------------------------------------
	/**
	 * The log command that wraps calls to the logger and translates log levels
	 */
	public class LogCommand implements Command {
		public Level translev;
		public Logger logger;
		
		public LogCommand(Logger _logger) {
			logger = _logger;
			//logger.info("log command");
		}
		
		public void setLogger(String lev) {
			//logger.info("setting logger "+lev);
			translev = translate(lev);
		}
		
		public Level translate(String lev) {
			switch (lev.toLowerCase()) {
			case "critical": return Level.SEVERE;
			case "error": return Level.SEVERE;
			case "warning": return Level.WARNING;
			case "info": return Level.INFO;
			case "debug": return Level.CONFIG;
			}
			return Level.INFO;
		}

		@Override
		public void execute(String arg0, BufferedReader arg1, BufferedWriter arg2) throws Py4JException, IOException {
			logger.log(translev,arg0);
		}
		public void execute(String msg) throws Py4JException, IOException {execute(msg,null,null);}

		@Override
		public String getCommandName() {return "execute";}//?
		@Override
		public void init(Gateway arg0, Py4JServerConnection arg1) {}

	}
	
	public class CommandEntryPoint {
		private LogCommand lc;

		public CommandEntryPoint(Logger logger) {
			logger.info("command entry point");
			lc = new LogCommand(logger);
		}
		public LogCommand getLogCommand(String str) {
			lc.setLogger(str);
			//lc.execute("request LC "+str);
			return lc;
		}
		public void log(String level,String message) throws Py4JException, IOException {
			lc.setLogger(level);
			lc.execute(message);
		}
		
	}
	//--------------------------------------------------------------------------------------------------------

	private enum Shell { 
		Python("PYTHONPATH","/usr/local/lib/python2.7/dist-packages/py4j/"), 
		Python3("PYTHONPATH","/usr/local/lib/python3.4/dist-packages/py4j"), 
		Bash("","");
		String env,path;
		Shell(String env,String path){this.env=env;this.path=path;}
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
		String[] command = new String[]{shell.name().toLowerCase(),processname,arg};
		ProcessBuilder processbuilder = new ProcessBuilder(command);
		if (shell.env != "" && shell.path != "") { 
			Map<String, String> env = processbuilder.environment();
			env.put(shell.env,shell.path); 
		}

		LOGGER.info(String.join(" ",command));
		processbuilder = new ProcessBuilder(command);
		processbuilder.redirectErrorStream(true);
		//processbuilder.redirectOutput(Redirect.appendTo(LOGGER));
		return processbuilder;
	}
	
	/**
	 * Initialises ProcessBuilder returning output from requested script 
	 * @param arg Argument to the aims_extract.sh script indicating transfer, process or reject
	 * @return
	 * @throws Exception 
	 */
	public String runProcess(String arg) { 
		LOGGER.fine("process out "+arg);
		StringBuilder sb1 = new StringBuilder();
		//Start a java_gateway for logging and autoclose when done
		try (GatewayWrapper gwwrapper = new GatewayWrapper(LOGGER)){
			sb1.append(arg+DELIM);
			sb1.append(readProcessOutput(getProcessBuilder(arg),DELIM));
		}
		catch (Exception e) {
			LOGGER.log(java.util.logging.Level.WARNING,"error logging from py module",e);
		}
		return sb1.toString();
	}
	
	/**
	 * Run and read the results of the script call
	 * @param pb Process Builder to start
	 * @param delimiter
	 * @return
	 */
	public String readProcessOutput(ProcessBuilder pb, String delimiter) {
		StringBuffer sb2 = new StringBuffer();	
		String line;
		try {
			Process process = pb.start();
			final InputStream is = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			do {
				while ((line = reader.readLine()) != null) { 
					sb2.append(line+delimiter);
					//LOGGER.info("PY>"+line);
				}
			} 
			while (process.isAlive());
			LOGGER.info("process out "+sb2.toString());
			if (sb2.length()==0){
				sb2.append("No return value. Process exit_code="+process.exitValue()+delimiter);
			}
		}
		catch (IOException ioe) {
			LOGGER.warning("Error starting and reading process output "+ioe);
		}
		return sb2.toString();
		
	}
	
	public String toString(){
		return "ProcessControl::" + processname;
	}
	public static void main(String[] args) {
		File testfile = new File("test/linz_admin_boundaries_logging_test.py");
		ProcessControl pc = new ProcessControl(testfile);
		pc.runProcess("load");
	}
	
}


