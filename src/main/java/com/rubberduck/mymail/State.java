package com.rubberduck.mymail;

/*
 * Enum 	: State
 * Purpose	: Defines the states of an email 
 */

public enum State {
	NOT_SENT,			// Not yet attempted 
	SENT, 				// Successfully sent
	FAILED;				// Attempted and failed
}
