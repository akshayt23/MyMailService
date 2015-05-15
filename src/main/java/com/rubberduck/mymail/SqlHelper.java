package com.rubberduck.mymail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.*;

/*
 * Class 	: SqlHelper
 * Purpose	: Helper class to facilitate database operations
 */

public class SqlHelper {
	static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";			// DB driver
	//static final String DB_URL = "jdbc:mysql://localhost/emails_db";	// DB URL

	static String username;					// DB username
	static String password;					// DB password
	static String dbUrl;					// DB URL
	static String mailsTable;				// Table which holds the mails
	
	private Connection con;					// Connection object

	public SqlHelper() {
		try {
			// Register JDBC driver
			Class.forName(JDBC_DRIVER);
			// Open a connection
			con = DriverManager.getConnection(dbUrl, username, password);

		} catch (SQLException se) {
			System.out.println("\nCould not connect to database!");
			System.out.println(se.getMessage());
			System.exit(-1);
		} catch (ClassNotFoundException e) {
			System.out.println(e.getMessage());
			System.exit(-1);
		}
	}
	
	// Gets user input related to database
	public static void getInput() {
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		try {
			System.out.print("Enter database username : ");
			username = br.readLine();
			System.out.print("Enter database password : ");
			password = br.readLine();
			
			System.out.print("Enter database name : ");
			dbUrl = "jdbc:mysql://localhost/" + br.readLine().replaceAll("\\s", "");
			System.out.print("Enter table name to store mails : ");
			mailsTable = br.readLine().replaceAll("\\s+", "");
			
		} catch (IOException e) {
			System.out.println(e);
		}
	}

	// Returns the connection object
	public Connection getConnection() {
		return con;
	}
	
	// Closes the connection
	public void closeConnection() {
		try {
			if (con != null)
				con.close();
		} catch (SQLException e) {
			System.out.println("Could not close connection to database!");
			System.out.println(e.getMessage());
		}
	}
	
	// Creates a new table for holding the mails
	public int createTable() {
		String query = "CREATE TABLE IF NOT EXISTS " + mailsTable +
				"(id int not null auto_increment primary key, " + 
				"sender varchar(40) not null, " + 
				"recipient varchar(40) not null, " + 
				"subject varchar(255), " + 
				"body text, " +
				"sent int not null);";
		
		if (con == null)
			return -1;

		int result = -1;
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			result = stmt.executeUpdate(query);
		} catch (SQLException e) {
			System.out.println("Could not create table!");
			System.out.println(e.getMessage());
			System.exit(-1);
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());			}
		}
		return result;
	}

	// Deletes the table used for holding mails
	public int dropTable() {
		String query = "DROP TABLE IF EXISTS " + mailsTable;

		if (con == null)
			return -1;

		int result = -1;
		Statement stmt = null;
		try {
			stmt = con.createStatement();
			result = stmt.executeUpdate(query);
		} catch (SQLException e) {
			System.out.println("Could not delete " + mailsTable + "!");
			System.out.println(e.getMessage());
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());			}
		}
		return result;
	}

	/*
	 * Function : execute
	 * Input	: A query in the form of a PreparedStatement object
	 * Purpose	: Executes the given query and returns the result
	 * Returns	: A ResultSet object containing the results of the query
	 */
	public ResultSet execute(PreparedStatement query) {
		ResultSet rs = null;
		// Execute query
		try {
			rs = query.executeQuery();
		} catch (SQLException e) {
			System.out.println("Failed to query database!");
			System.out.println(e.getMessage());
		}
		
		return rs;
	}

	/*
	 * Function : insertBulkMails
	 * Input	: The number of mails to be inserted
	 * Purpose	: Inserts the mails using a batch query to achieve improved performance
	 * Returns	: The number of mails that were inserted
	 */
	public int insertBulkMails(int count) {
		System.out.print("\nInserting dummy mails into database... ");
		
		PreparedStatement stmt = null;
		int[] arr = {};
		
		try {
			con.setAutoCommit(false);			// set autocommit to false for batch queries
			stmt = con.prepareStatement("INSERT INTO " + mailsTable + " "
					+ "(sender, recipient, subject, body, sent)"
					+ "VALUES (?, ?, ?, ?, ?)");
		
			for (int i=1; i<=count; i++) {
				stmt.setString(1, "sender@localhost.com");
				stmt.setString(2, "recepient" + i + "@localhost.com");
				stmt.setString(3, "Subject " + i);
				stmt.setString(4, "Body " + i);
				stmt.setInt(5, State.NOT_SENT.ordinal());

				stmt.addBatch();
			}
			
			arr = stmt.executeBatch();
			con.commit();
			
		} catch (SQLException e) {
			System.out.println("Could not insert mails to " + mailsTable + "!");
			System.out.println(e.getMessage());

		} finally { 
			try {
				con.setAutoCommit(true);
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}
		
		System.out.println("Done!");
		return arr.length;
	}
	
	/*
	 * Function : setState
	 * Input	: low (lower limit of range of mail Ids)
	 * 			  high (upper limit of range of mail Ids)
	 * 			  state (the state to be set for the given mails)
	 * Returns	: Number of rows updated
	 */
	public int setState(int low, int high, State state) {
		PreparedStatement stmt = null;
		int result = -1;

		if (con == null)
			return -1;

		try {
			stmt = con.prepareStatement("UPDATE " + mailsTable + " SET sent=? "
					+ "WHERE id>=? and id<=?");
			stmt.setInt(1, state.ordinal());
			stmt.setInt(2, low);
			stmt.setInt(3, high);
			
			result = stmt.executeUpdate();

		} catch (SQLException e) {
			System.out.println("Could not update mails state!");
			System.out.println(e.getMessage());
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}
		return result;
	}
	
	/*
	 * Function : setState
	 * Input	: failed (a list of all mail Ids whose state needs to be updated)
	 * 			  state (the state to be set for the given mails)
	 * Returns	: Number of rows updated
	 */
	public int setState(List<Integer> failed, State state) {
		PreparedStatement stmt = null;
		int result = -1;

		if (con == null)
			return -1;

		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (int i=0; i<failed.size()-1; i++) 
			sb.append(failed.get(i) + ", ");
		sb.append(failed.get(failed.size()-1) + ")");
		
		try {
			stmt = con.prepareStatement("UPDATE " + mailsTable + " SET sent=? "
					+ "WHERE id in " + sb.toString());
			stmt.setInt(1, state.ordinal());
			result = stmt.executeUpdate();

		} catch (SQLException e) {
			System.out.println("Could not update mails state!");
			System.out.println(e.getMessage());
			//e.printStackTrace();
		} finally {
			try {
				if (stmt != null)
					stmt.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());			}
		}
		return result;
	}

	/*
	 * Function : getMailsFromResultSet
	 * Input 	: A ResultSet object 
	 * Purpose	: Iterates through the results and creates Mail objects using the details
	 * Returns	: A list of mail objects
	 */
	public List<Mail> getMailsFromResultSet(ResultSet rs) {
		List<Mail> mails = new ArrayList<Mail>();
		
		// If ResultSet is null, return empty List
		if (rs == null)
			return mails;

		try {
			while (rs.next()) {
				int id = rs.getInt("id");
				String sender = rs.getString("sender");
				String recipient = rs.getString("recipient");
				String subject = rs.getString("subject");
				String body = rs.getString("body");

				Mail mail = new Mail(id, sender, recipient, subject, body);
				mails.add(mail);
			}
		} catch (SQLException e) {
			System.out.println("Could not read data!");
			System.out.println(e);
		}
		
		return mails;
	}
	
	// Returns the count of mails which have not been sent yet/successfully
	public int getFailedCount() {
		int count = 0;
		PreparedStatement stmt;
		ResultSet rs;
		try {
			stmt = con.prepareStatement( "SELECT COUNT(*) as rowcount" +
						" FROM " + mailsTable + " WHERE sent!=" + State.SENT.ordinal());
			rs = execute(stmt);
			rs.next();
			count = rs.getInt("rowcount");
			rs.close();
		} catch (SQLException e) {
			System.out.println("Could not get failed mails count!");
			System.out.println(e.getMessage());
		}
		
		return count;
	}

}
