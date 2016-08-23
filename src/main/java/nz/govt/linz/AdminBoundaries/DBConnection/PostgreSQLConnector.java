package nz.govt.linz.AdminBoundaries.DBConnection;

/**
 * This file is part of nz.co.kakariki.NetworkUtilities.
 * 
 * NetworkUtilities is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * NetworkUtilities is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

//import org.apache.log4j.Logger;

/**
 * Connector implementation for the test database. 
 * @author jnramsay
 *
 */
public class PostgreSQLConnector implements Connector {

	//private static Logger jlog = Logger.getLogger("nz.co.nzcomms.capacitydb.database.MySQLConnector");

	ConnectionDefinitions connectiondefinitions;

	protected static final String DEF_USER = "postgres";
	protected static final String DEF_PASS = "********";//obv fake
	protected static final String DEF_HOST = "127.0.0.1";
	protected static final String DEF_PORT = "5432";
	protected static final String DEF_DBNM = "postgres";

	/*
	static {
	try{
		new JDCConnectionDriver("org.postgresql.Driver","jdbc:postgresql","pguser","pgpass");
	}
	catch(Exception e){
		System.err.println("Error building JDCConnectionDriver :: "+e);
		
	}
	}
	*/
	
	public PostgreSQLConnector(){
		this(ConnectionDefinitions.POSTGRESQL);
	}
	
	public PostgreSQLConnector(ConnectionDefinitions cd){
		setConnectionDefinitions(cd);
	}

	public void init(Map<String,String> params){
		String user = params.containsKey("user") ? params.get("user") : DEF_USER;
		String pass = params.containsKey("pass") ? params.get("pass") : DEF_PASS;
		String host = params.containsKey("host") ? params.get("host") : DEF_HOST;
		String port = params.containsKey("port") ? params.get("port") : DEF_PORT;
		String dbnm = params.containsKey("dbname") ? params.get("dbname") : DEF_DBNM;
		//System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
		//System.out.format("u=%s x=%s h=%s p=$s d=$s",user, pass, host, port, dbnm);
		//System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
		init(user, pass, host, port, dbnm);
	}
	
	public void init(String user, String pass, String host, String port, String dbnm){
		//jlog.info("Setup connection CDB :: "+url());
	
		try{
			new ConnectionDriver(driver(),url(host,port, dbnm), user, pass);
		}
		catch(Exception e){
			System.err.println("Error building ConnectionDriver :: "+e);
			
		}
	}

	@Override
	public Connection getConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:jdc:jdcpool");
	}


	public ConnectionDefinitions getConnectionDefinitions() {
		return connectiondefinitions;
	}

	public void setConnectionDefinitions(ConnectionDefinitions cd) {
		this.connectiondefinitions = cd;
	}

	private String url(String host, String port, String db){
		port = (String) ((port != "") ? port : connectiondefinitions.port());
		return String.format("%s://%s:%s/%s",connectiondefinitions.prefix(),host,port,db);
	}
	/*
	private String url(){
		return url(DEF_HOST,DEF_PORT,DEF_DBNM);
	}
	*/
	
	private String driver(){
		return connectiondefinitions.driver();
	}
	
	@Override
	public String toString(){
		return "CDB:"+connectiondefinitions.driver();
	}

}
