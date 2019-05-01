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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import py4j.Gateway;
import py4j.GatewayServer;
import py4j.Py4JException;
import py4j.Py4JNetworkException;
import py4j.Py4JServerConnection;
import py4j.commands.Command;

/**
 * Class to initiate and run system processes, n this case the included python script, linz_admin_boundaries_uploader.py
 * @author jramsay
 */
public class ProcessControl {
	
	private static final Logger LOGGER = Logger.getLogger( ProcessControl.class.getName() );
	
	//private static final String[] ALLOWED_ARGS = new String[] {"detect"};
	private static final Set<String> ALLOWED_ARGS = new HashSet<String>(Arrays.asList("detect"));
	//this is the path where the debian packager puts the py part of the app
	private final static String DABP = "webapps/ab/WEB-INF/scripts/linz_admin_boundaries_uploader.py";
	private static final String BREAK = "<br/>\n";
	private static final String DELIMITER = "###";
	private static Shell shell;
	private String[] extra_args = new String[0];
	
	
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
		//GWPORT useful for testing with multiple (zombie) instances
		//private final int GWPORT = 25300+(int)(new Random()).nextInt(50);
		GatewayServer gwserver;
		
		/**
		 * GatewayWrapper constructor sets base logger an starts gateway
		 * @param _logger
		 */
		private GatewayWrapper(Logger _logger) {
			boolean success = false;
			do {
				try {
					gwserver = new GatewayServer(new CommandEntryPoint(_logger));//,GWPORT);
					gwserver.start();
					success = true;
					LOGGER.info("JavaGateway start");
				}
				catch (Py4JNetworkException pne) {
					LOGGER.warning("Error starting gateway"+pne);
				}
			} 
			while (!success);
		}
		
		@Override
		public void close() throws Exception {
			gwserver.shutdown();
			LOGGER.info("JavaGateway shutdown");
		}
	}
	
	//--------------------------------------------------------------------------------------------------------
	/**
	 * LogCommand wraps calls to the logger and translates log levels
	 */
	public class LogCommand implements Command {
		public Level translev;
		public Logger logger;
		
		/**
		 * LogCommand constructor sets base logger
		 * @param _logger
		 */
		public LogCommand(Logger _logger) {
			logger = _logger;
			//logger.info("log command");
		}
		
		/** Actually just sets the logging level of the logger (for the next log command)
		 * @param lev String name of the python log level
		 */
		public void setLogger(String lev) {
			//logger.info("setting logger "+lev);
			translev = translate(lev);
		}
		
		/**
		 * Defines the translation between python logging levels to java.logging levels
		 * @param lev String name of the python log level
		 * @return
		 */
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

		/**
		 * Override of the gateway execute command.
		 * NB. Couldn't get straight calls to gateway non-overridden methods didn't work so; command+execute
		 */
		@Override
		public void execute(String arg0, BufferedReader arg1, BufferedWriter arg2) throws Py4JException, IOException {
			logger.log(translev,arg0);
		}
		
		/** Wrapper around overriden execute setting reader/writer to null since they're not needed in this context*/
		public void execute(String msg) throws Py4JException, IOException {execute(msg,null,null);}

		/** Required mapping for the command though not sure why it would be needed */
		@Override
		public String getCommandName() {return "execute";}//?
		
		/** Null init not needed */
		@Override
		public void init(Gateway arg0, Py4JServerConnection arg1) {}

	}
	/**
	 * This is the entrypoint class for the Gateway. Its what he py side of the arrangement
	 * sees and calls operations on. Here it wraps calls to the LogCommand
	 * @author jramsay
	 */
	public class CommandEntryPoint {
		private LogCommand lc;

		/**
		 * Constructor sets itself a new LogCommand with the supplied base logger
		 * @param logger
		 */
		public CommandEntryPoint(Logger logger) {
			logger.info("command entry point");
			lc = new LogCommand(logger);
		}
		
		/**
		 * Access method providing py side with an object to execute commands on,
		 * in this case, the "execute" command
		 * @param str
		 * @return
		 */
		public LogCommand getLogCommand(String str) {
			lc.setLogger(str);
			//lc.execute("request LC "+str);
			return lc;
		}
		
		/**
		 * Performs the double, doing a set log (level) action and then an execute.
		 * It isn't actually used because lc's execute can do this function
		 * @param level
		 * @param message
		 * @throws Py4JException
		 * @throws IOException
		 */
		public void log(String level,String message) throws Py4JException, IOException {
			lc.setLogger(level);
			lc.execute(message);
		}
		
	}
	//--------------------------------------------------------------------------------------------------------

	/**
	 * Enum for the different shell commands with some setup params where needed
	 * @author jramsay
	 */
	private enum Shell { 
		Python("PYTHONPATH","/usr/local/lib/python2.7/dist-packages/py4j/"), 
		Python3("PYTHONPATH","/usr/local/lib/python3.4/dist-packages/py4j"), 
		Bash("","");
		String env,path;
		
		/** Cons */
		Shell(String env,String path){this.env=env;this.path=path;}
		
		/** Returns shell type based on file suffix */
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
	
	/**
	 * Add extra arguments to the process command line. For now the only 
	 * independent arg that you can add to the args list is "detect". 
	 * load/transfer/reject/optional/meshblock/nzlocalities are covered by 
	 * unique states
	 * @param extras
	 */
	public void addProcessArgs(String[] extras) {
		Set<String> filtered = new HashSet<String>(Arrays.asList(extras));
		//Keep only the allowed extras and warn if invalid included
		filtered.retainAll(ALLOWED_ARGS);
		if (filtered.size() != extras.length) {
			Set<String> removed = new HashSet<String>(Arrays.asList(extras));
			removed.removeAll(ALLOWED_ARGS);
			LOGGER.warning("Unrecognised optional arguments provided; "+String.join(",",removed));
		}
		extra_args = Arrays.copyOf(filtered.toArray(), filtered.size(), String[].class);
	}
	
	/**
	 * Gets a new processbuilder based on the detected shell(enum), the process file name 
	 * and the args (if any) defined in the shell(enum)'s params
	 * @param args
	 * @return ProcessBuilder
	 */
	public static ProcessBuilder getProcessBuilder(String[] arglist) {
		String[] cmdlist = new String[]{shell.name().toLowerCase(),processname};
		String[] command = new String[cmdlist.length + arglist.length];
		System.arraycopy(cmdlist, 0, command, 0, cmdlist.length);
		System.arraycopy(arglist, 0, command, cmdlist.length, arglist.length);

		ProcessBuilder processbuilder = new ProcessBuilder(command);
		if (shell.env != "" && shell.path != "") {
			Map<String, String> env = processbuilder.environment();
			env.put(shell.env,shell.path);
			LOGGER.info("Setting ENV "+shell.env+"="+shell.path);
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
	 * @return stdio result string from running the process
	 * @throws Exception 
	 */
	public String runProcess(String arg) { 
		LOGGER.fine("process out "+arg);
		StringBuilder rslt = new StringBuilder();
		rslt.append(arg+BREAK);
		StringBuilder args = new StringBuilder();
		args.append(arg);
		for (String each : extra_args) {args.append(DELIMITER+each);}
		
		getProcessBuilder(args.toString().split(DELIMITER));
		
		
		//Start a java_gateway for logging and autoclose when done
		try (GatewayWrapper gwwrapper = new GatewayWrapper(LOGGER)){
			rslt.append(
				readProcessOutput(
					getProcessBuilder(args.toString().split(DELIMITER)),BREAK
				)
			);
		}
		catch (Exception e) {
			LOGGER.log(java.util.logging.Level.WARNING,"error logging from py module",e);
		}
		return rslt.toString();
	}
	
	/**
	 * Run the saved processbuilder and read the results of the script call
	 * @param pb Process Builder to start
	 * @param delimiter
	 * @return Result string
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
	/** simple test */
	public static void main(String[] args) {
		File testfile = new File("test/linz_admin_boundaries_logging_test.py");
		ProcessControl pc = new ProcessControl(testfile);
		pc.addProcessArgs(new String[] {"detect","fake"});
		pc.runProcess("load");
	}
	
}


