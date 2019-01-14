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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import static nz.govt.linz.AdminBoundaries.DABServlet.ABs;
import static nz.govt.linz.AdminBoundaries.DABServlet.ABIs;

import nz.govt.linz.AdminBoundaries.DABContainerComp.ImportStatus;
import nz.govt.linz.AdminBoundaries.DABContainerComp.TableInfo;

/**
 * Connector intermediate class handles database connectivity and file
 * read/write. Also does minimal post processing
 * 
 * @author jramsay
 *
 */
public class DABConnector {

	private static final Logger LOGGER = Logger.getLogger(DABConnector.class.getName());

	// private Connector connector;
	DataSource datasource = null;

	/**
	 * Constructor for DAB database DAO 
	 */
	public DABConnector() {
		initDataSource();
	}
	
	/**
	 * Initialise a new data source object 
	 * @return Datasource
	 */
	private void initDataSource() {
		try {
			datasource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/linz/aims");
		} catch (NamingException ne) {
			LOGGER.warning("Cannot locate datasource. " + ne);
		}
	}

	/**
	 * Datasource provided (eg for mocking)
	 * 
	 * @param datasource_m
	 */
	public DABConnector(DataSource datasource_m) {
		datasource = datasource_m;
	}

	/**
	 * Fetches summary data from temp import schema, admin_bdys_import
	 * 
	 * @return {@codeList<List<String>> representing a MxN data table}
	 */
	public List<List<String>> executeQuery(String query,boolean includehead) {
		List<List<String>> result = null;
		// System.out.println(String.format("### QRY ### %s", query));
		try {
			ResultSet rs = exeQuery(query);
			if (rs != null) {
				result = parseResultSet(rs,includehead);
				LOGGER.fine("RES Size "+result.size());
			}
		} 
		catch (SQLException sqle) {
			LOGGER.warning("SQLError (q) " + sqle + "\n" + query);
			result = parseSQLException(sqle);
		}
		return result;
	}
	public List<List<String>> executeQuery(String query){
		return executeQuery(query,true);
	}
	/**
	 * Local query wrapper
	 * 
	 * @param query
	 * @return
	 * @throws SQLException
	 */
	private ResultSet exeQuery(String query) throws SQLException {
		ResultSet result = null;
		try (Connection conn = datasource.getConnection()) {
			Statement stmt = conn.createStatement();
			if (stmt.execute(query)) {
				result = stmt.getResultSet();
				System.out.println("RES::"+result);
			}
		}
		return result;
	}

	/**
	 * Generic resultset-table to list-list formatter
	 * 
	 * @param rs
	 * @return
	 */
	private List<List<String>> parseResultSet(ResultSet rs, boolean includehead) throws SQLException {
		List<String> head_row;// = new ArrayList<String>();
		List<String> body_row;// = new ArrayList<String>();
		List<List<String>> table = new ArrayList<>();

		ResultSetMetaData rsmd = rs.getMetaData();
		int count = rsmd.getColumnCount();
		head_row = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			String l = rsmd.getColumnLabel(i);
			LOGGER.fine("Col Label " + l);
			head_row.add(l);
		}
		if (includehead) {
			table.add(head_row);
		}
		while (rs.next()) {
			body_row = new ArrayList<>();
			for (String col : head_row) {
				String v = rs.getString(col);
				LOGGER.fine("Row[" + col + "] = " + v);
				body_row.add(v);
			}
			table.add(body_row);
		}
		return table;
	}

	/**
	 * Error handler that formats a structured response for eventual display to the
	 * user
	 * 
	 * @param sqle
	 * @return
	 */
	private List<List<String>> parseSQLException(SQLException sqle) {
		List<List<String>> result = null;

		// Error is written to general log and result is returned
		System.out.println("SQL error " + sqle);
		// return the error to the user
		result = new ArrayList<>();
		List<String> line = new ArrayList<>();
		line.add("SQLException");
		line.add(sqle.toString());
		result.add(line);
		return result;
	}

	/**
	 * Single-quotes (v) column names with spaces in them eg ,a b, => ,'a b',
	 * 
	 * @param columns
	 * @return
	 */
	public String quoteSpace(String columns) {
		LOGGER.fine("COLS. " + columns);
		StringBuilder res = new StringBuilder();
		for (String col : columns.split(",")) {
			if (col.trim().indexOf(" ") > 0) {
				res.append("'" + col + "'");
			} else {
				res.append(col);
			}
			res.append(",");
		}
		return res.deleteCharAt(res.lastIndexOf(",")).toString();
	}

	/**
	 * Get the column type for a named column using table_version function
	 * ver_table_key_dattype
	 * 
	 * @param tablename
	 * @param colname
	 * @return
	 */
	protected String colType(String tablename, String colname) {
		String query = String.format("SELECT table_version.ver_table_key_datatype('%s','%s')", tablename, colname);
		String coltype = "";
		try {
			ResultSet rs = exeQuery(query);
			rs.next();
			coltype = rs.getString(1);
		}
		catch (SQLException sqle) {
			LOGGER.warning("Col Type "+query+" failed " + sqle);
		}
		return coltype;
	}

	// -------------------------------------------------------------------------

	/**
	 * Return htmltable containing table row count
	 * 
	 * @param schema
	 * @param table
	 * @return
	 */
	public String compareTableCount(String schema, String table) {
		// read admin_bdys diffs
		if (table == null) {
			return DABContainerComp.DEF_TABLE;
		} else {
			String query = String.format("SELECT COUNT(*) count FROM %s.%s", schema, table);
			return DABFormatter.formatTable(table, executeQuery(query));
		}
	}

	/**
	 * Use table_version get_diff func to return differences between the temp and
	 * destination tables
	 * 
	 * @param ti
	 * @return
	 */
	public String compareTableData(TableInfo ti) {
		// read table diffs
		String t1 = String.format("%s.%s", ABs, ti.dst());
		String t2 = String.format("%s.%s", ABIs, ti.tmp());
		String rec = String.format("T(code char(1), id %s)", colType(ABs + "." + ti.dst(), ti.key()));
		String query = String.format("SELECT T.id, T.code FROM table_version.ver_get_table_differences('%s','%s','%s') as %s",
				t1, t2, ti.key(), rec);
		return "<article><p>"+query+"</p>" + DABFormatter.formatTable(ti.dst(), executeQuery(query)) + "</article>";
	}

	/**
	 * Determine state of database by testing for tables temp_X, snap_X and dst=snap
	 * 
	 * @return ImportStatus for selected table TODO rewrite test for mapped status
	 *         reflecting geo ops instead of snap build
	 */
	public ImportStatus getStatus(TableInfo ti) {
		// check that imported temp files exist
		String exist_query = String.format(
				"SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='%s' AND table_name='%s')", ABIs,
				ti.tmp());
		boolean exist_res = false;
		try {
			ResultSet rs = exeQuery(exist_query);
			rs.next();
			exist_res = rs.getBoolean(1);
			LOGGER.finer("1CQa " + exist_query + " / " + exist_res);
		}
		catch (SQLException sqle) {
			LOGGER.warning("1CQb failed " + exist_query + " / " + sqle);
		}
		LOGGER.finer("1TQ " + exist_query + " / " + exist_res);
		if (exist_res) {
			// get original column names (so column order isn't considered in comparison
			String col_query = String.format("select array_to_string(array_agg(column_name::text),',') "
					+ "from information_schema.columns " + "where table_schema='%s' " + "and table_name='%s'", ABs, ti.dst());
			String columns = "";
			try {
				ResultSet rs = exeQuery(col_query);
				rs.next();
				columns = quoteSpace(rs.getString(1));
			}
			catch (SQLException sqle) {
				LOGGER.warning("1CQc failed " + col_query + " / " + sqle);
			}
			
			LOGGER.finer("2CQa " + col_query + " / " + columns);
			// tmp files match dst files
			String tt = String.format("SELECT %s FROM %s.%s", columns, ABIs, ti.tmp());
			String dt = String.format("SELECT %s FROM %s.%s", columns, ABs, ti.dst());
			String cmp_query = String.format("SELECT NOT EXISTS (%s EXCEPT %s UNION %s EXCEPT %s)", tt, dt, dt, tt);
			
			try {
				ResultSet rs = exeQuery(cmp_query);
				rs.next();
				boolean cmp_res = rs.getBoolean(1);
				LOGGER.finer("3CQa " + cmp_query + " / " + cmp_res);
				if (cmp_res) {
					return ImportStatus.COMPLETE;
				}
			}
			catch (SQLException sqle) {
				LOGGER.warning("3CQb failed " + cmp_query + " / " + sqle);
			}
			return ImportStatus.LOADED;
		}
		return ImportStatus.BLANK;
	}

	public String toString() {
		return "DABConnector::";// +connector;
	}

	/**
	 * main method used for testing
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		DABConnector dabc = new DABConnector();
		System.out.println(dabc.executeQuery("select 1"));
	}

}
