/*
 * Course: CSCI-565-Distributed Computing Systems
 *
 * Student: Henri van den Bulk
 */
package org.mines.p1;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

/**
 * The Client is responsible for communicating with the {@link Server}
 * Based on the Command Line Options the client will send the content of a file or
 * ask the server for the next item in the queue.
 * 
 * The Client and Server leverage some of the STOMP commands for an abstract communication level
 * 
 * Basic Communication Flow:
 * 
 * SEND - In the case of sending a file the Client to Server follows this flow:
 * 
 * C --> CONNECT --> S, S --> CONNECTED --> C
 * C --> SEND --> S, S --> RECEIPT --> C
 * C --> DISCONNECT --> S, S --> RECEIPT --> C
 * 
 * RECEIVE - The client initiates a flow of wanting to subscribe to a queue on the server. 
 * 
 * C --> CONNECT --> S, S --> CONNECTED --> C
 * C --> SUBSCRIBE(Queue) --> S, S --> MESSAGE(queue entry) --> C
 * C --> DISCONNECT --> S, S --> RECEIPT --> C
 * 
 * @see <a href="https://stomp.github.io/stomp-specification-1.2.html">STOMP Spec</a>
 */
@Slf4j
public final class Client {

    private String hostname;
    private int port;

    private Channel channel = null;
    private Bootstrap b = null;
    private EventLoopGroup group = null;
    private ChannelFuture lastWriteFuture = null;
	private static String QUEUE_NAME = "/messages";

	protected boolean connected = false;

	
    public Client() {
    	this("localhost", 55555);
    	
	}
    
    public Client(String hostname, int port) {
		this.hostname = hostname;
		this.port = port;
	}
	/**
	 * Send a message to the server and can specify a destination (queue)
	 * where to place the message.
	 *
	 * @param dest
	 *            The name of the channel to send the message to
	 * @param mesg
	 *            The message to send.
	 */
	public void send(String dest, String mesg) {
		send(dest, mesg, null);
	}
    
	/**
	 * Wrapper function for sending a message
	 * 
	 * @param dest The queue/topic to which the client wants to send a message to
	 * @param mesg Content of the message, e.g. body of the message
	 * @param header The headers
	 */
	private void send(String dest, String mesg, Map<String, String> header) {
		if (header == null)
			header = new HashMap<String, String>();
		header.put("destination", dest);
		transmit(Command.SEND, header, mesg);
	}

	/**
	 * Wrapper Function to transmit a message to the server using the current
	 * channel. If the channel does not exist nothing will be send
	 * 
	 * @param c Command
	 * @param h Headers
	 * @param b Body
	 */
	private void transmit(Command c, @SuppressWarnings("rawtypes") Map h, String b) {
		log.debug("Transmitting message");

		if (channel != null && channel.isActive()) {
			Message msg = new Message(c, h, b);

			lastWriteFuture = channel.writeAndFlush(msg);
			// We're adding a listener that gets notified by the event loop when
			// the operation is complete.
			lastWriteFuture.addListener(new ChannelFutureListener()
		        {
		                public void operationComplete(ChannelFuture future) throws Exception
		                {
		                    if (future.isSuccess()) { log.debug("Send Completed"); }
		                    else { log.warn("FAILURE" + future.cause());   }
		                }
		        });
						
		} else {
			log.warn("There is no open channel with the server");
		}
	}
	
	/**
	 * The client can subscribe to a queue and will receive the oldest message
	 * in the queue. 
	 * @param queue Name of the queue to subscribe to.
	 */
	public void subscribe(String queue) {
		HashMap<String, String> h = new HashMap<String, String>();
		h.put("destination", queue);
		
		transmit(Command.SUBSCRIBE, h, null);
		
		try {
			// Wait for the connection to be closed
			channel.closeFuture().sync();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * The client will initiate the connect to the server.
	 * This will start the event loop for communications between the client
	 * and the server.
	 */
	public void connect() {

		group = new NioEventLoopGroup();
		b = new Bootstrap();
			    
	    b.group(group)
	     .channel(NioSocketChannel.class)
	     .handler(new ClientInitializer());
	
		//			channel = b.connect(HOST, PORT).sync().channel();
		try {
			channel = b.connect(hostname, port).sync().channel();
			connected = true;
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}    	
	}
	
	/**
	 * Close up the connection and do any clean up.
	 * We'll wait to make sure we don't have any outstanding communications or
	 * messages that might be in the send buffer.
	 */
	public void close() {
		if (!isConnected())
			return;
		transmit(Command.DISCONNECT, null, null);

		connected = false;
		
        // Wait until all messages are flushed before closing the channel.
        if (lastWriteFuture != null) {
            try {
				lastWriteFuture.sync();
				group.shutdownGracefully();

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
	}

	public boolean isConnected() {
		return connected;
	}
	
	/**
	 * Create the Command Line Options
	 * 
	 * @return Options that can be used for command line options
	 */
	private static Options createOptions() {
		final Options options = new Options();

		options.addOption(Option.builder("s").hasArgs().required(false)
				.desc("The server name to where we're connecting").argName("server").build());
		options.addOption(Option.builder("p").hasArgs().required(false).desc("Specific port number on the server")
				.argName("port").build());

		options.addOption(Option.builder("c").hasArgs().required(false).desc("Command [receive|send]")
				.argName("command").build());

		options.addOption(Option.builder("f").hasArgs().required(false).desc("File name for the send command")
				.argName("file").build());

		return options;
	}

	/**
	 * Based on the defined options prints out the "usage" of the command line
	 * options
	 * 
	 * @param options
	 */
	private static void usage(Options options) {

		// Use the inbuilt formatter class
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("p1client", options);
	}

	/**
	 * This utility function will read a file from the file system and returns
	 * the content of the file into a single string.
	 * 
	 * @param fileName
	 *            Full name of the file that needs to be read
	 * @return A string representing the content of the file
	 */
	public static String readFromFile(String fileName) {
		File file = new File(fileName);
		log.info("Reading file: " + fileName);
		try {
			return FileUtils.readFileToString(file);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "";
		}
	}
	
    public static void main(String[] args) throws Exception {
		Logger log = LoggerFactory.getLogger("main");

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		Options options = createOptions();
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e1) {
			// TODO Auto-generated catch block
			usage(options);
			log.debug("Unable to parse CLI" + args);
			System.exit(1);
		}

		int port = Integer.parseInt(cmd.getOptionValue("p", "55555"));
		String server = cmd.getOptionValue("s", "127.0.0.1");
		String fileName = cmd.getOptionValue("f", "");
		String command = cmd.getOptionValue("c", "");

        try {
 
            Client client = new Client(server, port);
            client.connect();

            // Check the argument and perform the respective actions in the client.
            if ("send".equals(command.toLowerCase())) {
            	log.info("Sending file [{}] to queue [{}]", fileName, Client.QUEUE_NAME);
				client.send(Client.QUEUE_NAME, Client.readFromFile(fileName));
            } else if ("receive".equals(command.toLowerCase())) {
            	client.subscribe(Client.QUEUE_NAME);
            }

            client.close();
        } finally {
            log.info("All done");

        }
    }
}
