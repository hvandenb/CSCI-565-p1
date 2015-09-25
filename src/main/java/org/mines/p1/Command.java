/*
 * Course: CSCI-565-Distributed Computing Systems
 *
 * Student: Henri van den Bulk
 */
package org.mines.p1;

/**
 * Using STOMP commands {@link https://stomp.github.io/stomp-specification-1.2.html}
 * 
 * @author Henri van den Bulk
 *
 */
public enum Command {
	/**
	 * Command
	 */
	    STOMP,
	    CONNECT,
	    CONNECTED,
	    SEND,
	    SUBSCRIBE,
	    UNSUBSCRIBE,
	    ACK,
	    NACK,
	    BEGIN,
	    DISCONNECT,
	    MESSAGE,
	    RECEIPT,
	    ERROR,
	    UNKNOWN
	}
