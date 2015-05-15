package com.rubberduck.mymail;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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

import com.sun.mail.smtp.SMTPAddressFailedException;
import com.sun.mail.smtp.SMTPSendFailedException;

/* 
 * Class 	: SMTPMailer
 * Purpose 	: Helper class to establish a connection with the SMTP server and send out mails
 * 			  Contains methods for the above functionalities 
 */

public class SMTPMailer {

	Properties mailProperties;				// Properties object to define SMTP properties
	Session mailSession;					// Session object
	MimeMessage mailMessage;				// MimeMessage to be sent out
	Transport transport;					// Transport object used to send message
	String host;							// SMTP host
	

	// Constructor
	public SMTPMailer(String host) {
		mailSession = null;
		mailMessage = null;
		transport = null;

		this.host = host;
		
		// Setup Mail Server Properties
		mailProperties = System.getProperties();
		mailProperties.put("mail.smtp.host", host); // set SMTP host
		mailProperties.put("mail.smtp.port", "1123"); // set TLS port
		mailProperties.put("mail.smtp.auth", "true"); // enables authentication
		mailProperties.put("mail.smtp.starttls.enable", "true"); // enable STARTTLS

	}
	
	/*
	 * Function : authenticateAndConnect
	 * Input	: User credentials for authentication
	 * Purpose	: Authenticates the session and connects to the SMTP server
	 * Returns	: True if success, false otherwise
	 */
	
	public boolean authenticateAndConnect(final String user, final String pass) {
		boolean retval = false;
		
		if (transport == null || !transport.isConnected()) {
			try {
				// Get mail session using credentials
				mailSession = Session.getDefaultInstance(mailProperties, new Authenticator() {
					 @Override
                     protected PasswordAuthentication getPasswordAuthentication() {
                         return new PasswordAuthentication(user, pass);
                     }
				});

				// Connect to SMTP server
				transport = mailSession.getTransport("smtp");
				transport.connect(host, user, pass);
				
				// Successfully established connection
				retval = true;				
			
			} catch (MessagingException me) {
				// Failed to establish a connection
				return false;
			}
		}

		return retval;
	}
	
	/* Function : sendEmails
	 * Input	: List of mails to be sent out
	 * Purpose	: Tries sending the mails to the SMTP server
	 * 			  If unable to send a mail, adds it to a failed list
	 * 			  If connection to SMTP server is lost, adds mails which were not processed 
	 * 				to an unprocessed mails list
	 * 			  Sends back information about mails which were not sent/processed
	 * Returns	: void
	 */
	
	public void sendEmails(final List<Mail> mails, 
			List<Integer> failedMailIds, List<Integer> unProcessedMailIds) {
		
		// Start sending mails one by one
		for (int i=0; i<mails.size(); i++) {		
			Mail mail = mails.get(i);
			try {
				// Generate Mail Message
				if (transport.isConnected()) {

					mailMessage = new MimeMessage(mailSession);
					mailMessage.setFrom(new InternetAddress(mail.getSender()));
					mailMessage.addRecipient(Message.RecipientType.TO,
						new InternetAddress(mail.getRecipient()));
					mailMessage.setSubject(mail.getSubject());
					mailMessage.setContent(mail.getBody(), "text/html");

					// Send message
					transport.sendMessage(mailMessage, mailMessage.getAllRecipients());
				}
			} catch (AddressException e) {
				failedMailIds.add(mail.getId());
				System.out.println(e);
			} catch (AuthenticationFailedException e) {
				failedMailIds.add(mail.getId());
				System.out.println(e);
			} catch (MessagingException e) {
				// Connection to SMTP server lost, mark remaining mails as NOT_SENT
				for (i=i; i<mails.size(); i++)
					unProcessedMailIds.add(mails.get(i).getId());
			}
		}
		
		// All done, close the connection to the SMTP server
		if (transport != null) {
			try {
				transport.close();
			} catch (MessagingException e) {
				System.out.println("Could not close connection to SMTP server");
				System.out.println(e.getMessage());
			}
		}
	}
}
