/*
 * Course: CSCI-565-Distributed Computing Systems
 *
 * Student: Henri van den Bulk
 */

package org.mines.p1;

import java.io.Serializable;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Base structure of a message that's being exchanged. This is a Data POJO
 * 
 * @author Henri van den Bulk
 *
 */
@Data
@AllArgsConstructor
public class Message implements Serializable  {
	private Command command;
	private Map headers;
	private String body;
}
