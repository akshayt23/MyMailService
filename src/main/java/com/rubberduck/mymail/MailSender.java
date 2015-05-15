package com.rubberduck.mymail;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.mysql.jdbc.authentication.MysqlClearPasswordPlugin;

/* 
 * Class 	: MailSender
 * Purpose  : Defines a new thread which can be used as a seperate point of execution to 
 * 			  fetch mails from the database and send them out
 */

public class MailSender implements Runnable {

	int threadId;					// unique id for the thread
	int batchSize;					// size of batch in which mails are brought into memory
	SqlHelper sqlHelper;			// helper object for database operations

	// Constructor
	public MailSender(int threadId, int batchSize) {
		// TODO Auto-generated constructor stub
		this.threadId = threadId;
		this.batchSize = batchSize;
		sqlHelper = new SqlHelper();
	}

	/* 
	 * Function : run
	 * Input	: none
	 * Purpose	: Entry point for execution of thread.
	 * 			  Fetches [batchSize] mails into memory and sends them out
	 * Returns	: void 
	 */
	public void run() {
		System.out.println("Thread " + threadId + " : Started");
		
		// Fetch mails to be sent
		List<Mail> mails = fetchMails();
		
		while (mails.size() > 0) {
			System.out.println("Thread " + threadId + " : Sending next batch... ");
			
			SMTPMailer smtpMailer = new SMTPMailer("localhost");

			// LinkedLists to hold the id's of mails that could not be sent/processed
			List<Integer> failedMailIds = new LinkedList<Integer>();
			List<Integer> unProcessedMailIds = new LinkedList<Integer>();

			// Attempt to authenticate and connect to SMTP server
			if (!smtpMailer.authenticateAndConnect("user", "pass")) {
				// Connection failed
				
				System.out.println("Thread " + threadId + " : Could not connect to SMTP server." +
						" Please make sure that the server is running and then try again.");
				
				// Revert fetched mails to NOT_SENT
				Mail firstMail = mails.get(0);
				Mail lastMail = mails.get(mails.size() - 1); 
				sqlHelper.setState(firstMail.getId(), lastMail.getId(), State.NOT_SENT);
				
				sqlHelper.closeConnection();
				System.out.println("Thread " + threadId + " : Exiting");
				
				return;
				
			} else {			
				// Connection established successfully
				
				// Send out mails
				smtpMailer.sendEmails(mails, failedMailIds, unProcessedMailIds);
				
				// Mark failed emails
				if (failedMailIds != null && failedMailIds.size() > 0)
					sqlHelper.setState(failedMailIds, State.FAILED);
				// Mark un-processed emails
				if (unProcessedMailIds != null && unProcessedMailIds.size() > 0)
					sqlHelper.setState(unProcessedMailIds, State.NOT_SENT);
			
				// Fetch next batch of mails
				mails = fetchMails();
			}
		}
		
		sqlHelper.closeConnection();
		System.out.println("Thread " + threadId + " : Exiting");
	}
	
	
	/*
	 * Function	: fetchMails
	 * Input	: none
	 * Purpose	: Reads the next [batchSize] mails from the database, creates a list of mails
	 * 				using the ResultSet obtained and marks the mails as SENT
	 * Returns	: A list of mails fetched from the database
	 */
	public List<Mail> fetchMails() {
		Connection con = sqlHelper.getConnection();
		PreparedStatement query = null;
		ResultSet rs = null;
		List<Mail> mails = null;
		try {
			if (con != null) {
				// Synchronize the query and updation logic to avoid race conditions
				synchronized(SqlHelper.class) {
					
					// Query database to fetch [batchSize] unsent mails
					query = con.prepareStatement("SELECT * FROM " + SqlHelper.mailsTable + 
						" WHERE sent = " + State.NOT_SENT.ordinal() + " LIMIT " + batchSize);
					rs = sqlHelper.execute(query);
				
					mails = sqlHelper.getMailsFromResultSet(rs);
					if (mails.size() > 0) {	
						Mail firstMail = mails.get(0);
						Mail lastMail = mails.get(mails.size() - 1); 
					
						// Mark fetched mails as SENT
						sqlHelper.setState(firstMail.getId(), lastMail.getId(), State.SENT);
					}
				}
			}
		} catch (SQLException e) {
			System.out.println("Could not fetch mails!");
			System.out.println(e);
			System.exit(-1);
		} finally {
			try {
				if (rs!= null) rs.close();
				if (query!= null) query.close();
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}
		
		return mails;
	}
	
}
