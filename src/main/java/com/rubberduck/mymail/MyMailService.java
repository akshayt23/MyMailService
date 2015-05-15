package com.rubberduck.mymail;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import javax.mail.*;
import javax.mail.internet.*;

import java.sql.*;

/* 
 * Class 	: MyMailService
 * Purpose	: Main class which starts all functionalities required
 */

public class MyMailService {
	
	/* Function : main
	 * Input	: args[] (command line arguments)
	 * Purpose	: Main entry point of the program.
	 * 			  Calls appropriate functions to accept user input, cleanup database, 
	 * 				insert dummy mails to table and send mails using thread(s)
	 * Returns	: Void
	 */
	public static void main(String[] args) throws IOException {
		int threadsCount;					// total number of threads
		int mailsCount;						// total number of mails

		// Input database parameters
		SqlHelper.getInput();
		
		// Get SqlHelper object and cleanup database
		SqlHelper sqlHelper = new SqlHelper();
		sqlHelper.dropTable();
		sqlHelper.createTable();

		// Get number of threads and mails from user
		threadsCount = inputValidInteger(
				"\nEnter total number of threads : ", 1, 20);
		mailsCount = inputValidInteger(
				"Enter total number of mails : ", 1, Integer.MAX_VALUE);

		// Insert dummy mails to the database
		sqlHelper.insertBulkMails(mailsCount);
		
		// Start sending mails using [threadsCount] threads
		startSendingMails(mailsCount, threadsCount);
		
		// Fetch number of mails which were not sent and see if user wants to retry
		int failedCount = processFailedMails(sqlHelper, threadsCount);
		
		// All done, we can safely close the database connection now
		sqlHelper.closeConnection();
		
		System.out.println("\nSent " + (mailsCount-failedCount) + " mails!");
		System.out.println("Goodbye!\n");
	}
	
	/* 
	 * Function : startSendingMails
	 * Input 	: mailsCount (total number of mails to be sent)
	 * 		   	  threadsCount (total number of threads to be used)
	 * Purpose 	: Calculates the size of batch in which mails are to be sent (max. 1000).
	 * 			  Initializes a thread pool of [threadsCount] threads.
	 * 			  Starts [threadCount] MailSender threads which send out the mails.
	 * Returns 	: Void  
	 */
	public static void startSendingMails(int mailsCount, int threadsCount) {
		ExecutorService executor = Executors.newFixedThreadPool(threadsCount); 
		System.out.println("\nSending mails...\n");
		if (mailsCount > 0) {
			
			int batchSize = getBatchSize(mailsCount, threadsCount);
			
			// Start [threadsCount] MailSender threads
			for (int i=1; i<=threadsCount; i++) {
				Thread thread = new Thread(new MailSender(i, batchSize));
				executor.execute(thread);
			}
		}
		executor.shutdown();
		try {
			executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		} 

	}
	
	/*
	 * Function : processFailedMails
	 * Input	: SqlHelper object, number of threads
	 * Purpose	: Prompts the user to retry sending mails
	 * Returns	: Number of mails which are still not sent
	 */
	
	public static int processFailedMails(SqlHelper sqlHelper, int threadsCount) {
		// Fetch number of mails which were not sent
		int failedCount = sqlHelper.getFailedCount();
		// Check for failed mails and prompt user to retry
		while (failedCount > 0) {
			String in = inputValidString("\nFailed to send " + failedCount + " mails. " +
					"Do you want to retry again (yes/no)? ", 
					Arrays.asList("yes", "no"));
			if (in.compareTo("yes") == 0)
				startSendingMails(failedCount, threadsCount);
			else
				break;
			
			// Fetch number of mails which were not sent again after retrying
			failedCount = sqlHelper.getFailedCount();
		}

		return sqlHelper.getFailedCount();
	}
	
	/* Function : getBatchSize
	 * Input 	: mailsCount (total number of mails to be sent)
	 * 		   	  threadsCount (total number of threads to be used)
	 * Purpose	: Calculates size of batch in which mails are to be sent such that number of
	 * 			  mails in memory at a given instance across all threads is less than 
	 * 			  MAX_MAILS_IN_MEMORY (10000)
	 * Returns	: Size of batch that each thread can use
	 */
	
	public static int getBatchSize(int mailsCount, int threadsCount) {
		int MAX_MAILS_IN_MEMORY = 10000;
		int batchSize;
		
		// Can fetch all the mails together into memory, divide equally among threads
		if (mailsCount < MAX_MAILS_IN_MEMORY)
			batchSize = mailsCount / threadsCount;
		
		// Need to fetch mails in batches, limit batch size to follow memory constraints
		else
			batchSize = MAX_MAILS_IN_MEMORY / threadsCount;
		
		return batchSize;
	}
	
	/* Function : inputValidInteger
	 * Input 	: prompt (The prompt to be shown to the user)
	 * 			  low (the min. value in the accepted range)
	 * 			  high (the max. value in the accepted range)
	 * Purpose  : Makes sure the user enters the correct input in the desired range
	 * Returns	: Valid input by the user
	 */
	public static int inputValidInteger(String prompt, int low, int high) {
		boolean inputCorrect = false;
		int input = Integer.MIN_VALUE;
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		while (!inputCorrect) {
			try {
				System.out.print(prompt);
				input = (Integer.parseInt(br.readLine()));
				if (input < low || input > high)
					throw new Exception();
				else
					inputCorrect = true;
			} catch (Exception e) {
				System.out.println ("Please enter a positive number between " + 
						low + " and " + high);
			}
		}
		
		return input;
	}
	
	/* Function : inputValidString
	 * Input 	: prompt (The prompt to be shown to the user)
	 * 			  accepted (a list of strings which are valid inputs, 
	 * 							pass null if all strings are valid)
	 * Purpose  : Makes sure the user enters a valid string
	 * Returns	: Valid input by the user
	 */
	public static String inputValidString(String prompt, List<String> accepted) {
		boolean inputCorrect = false;
		String input = "";
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		while (!inputCorrect) {
			try {
				System.out.print(prompt);
				input = br.readLine();
				
				if (accepted == null)
					return input;
				
				if (!accepted.contains(input))
					throw new Exception();
				else
					inputCorrect = true;
			} catch (Exception e) {
				System.out.println ("Invalid input, please try again.");
			}
		}
		
		return input;
	}

}
