package com.rubberduck.mymail;

import java.util.*;

/* 
 * Class 	: Mail
 * Purpose	: Defines a Mail and its related properties
 */

public class Mail {
	private int id;					// unique id of mail
	private String sender;			// sender address
	private String recipient;		// recipient address
	private String subject;			// subject line of mail
	private String body;			// contents of mail
	
	
	// Constructor
	public Mail(int id, String sender, String recipient, String subject, String body) {
		
		this.id = id;
		this.sender = sender;
		this.recipient = recipient;
		this.subject = subject;
		this.body = body;
	}

	public int getId() {
		return id;
	}
	public String getSender() {
		return sender;
	}
	public String getRecipient() {
		return recipient;
	}
	public String getSubject() {
		return subject;
	}
	public String getBody() {
		return body;
	}
}
