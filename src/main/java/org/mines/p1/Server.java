/*
 * Course: CSCI-565-Distributed Computing Systems
 *
 * Student: Henri van den Bulk
 */
package org.mines.p1;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * The main Server is the entry point for starting the server
 */
@Slf4j
public final class Server {

	private int port;
	
	/**
	 * Default constructor and will assume localhost and port 55555 as the defaults
	 */
	public Server() {
		this("localhost", 55555);
	}
	
	public Server(String host, int port) {
		this.port = port;
	}

	public Server(int port) {
		this.port = port;
	}
    
	/**
	 * Main entry into the server. This will start the server and created two
	 * event loops. One is the main thread that listens on the port and 
	 * when an new connection is established it will hand the management of that
	 * connection off to the worker event loop.
	 */
	public void run(){
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.DEBUG))
             .childHandler(new ServerInitializer());

            // Bind and start to accept incoming connections.
            // The connection is synced, which means that the applicaiton will stay
            // in an infinite loop until the application is killed.
            try {

            	log.info("Server started [{}]", port);
				b.bind(port).sync().channel().closeFuture().sync();
				
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
		
	}
	
	/**
	 * Create the Command Line Options
	 * 
	 * @return Options that can be used for command line options
	 */
	private static Options createOptions() {
		final Options options = new Options();

		options.addOption(Option.builder("p").hasArgs().required(false).desc("Specific port number on the server")
				.argName("port").build());

		return options;
	}

	/**
	 * Based on the defined options prints out the "usage" of the command line
	 * options
	 * 
	 * @param options
	 */
	private static void usage(Options options) {

		// Use the built in formatter class
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("p1server", options);
	}
	
	
    public static void main(String[] args)  {
		// Setting a default port number.

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		Options options = createOptions();

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e1) {
			usage(options);
			System.exit(1);
		}

		int portNumber = Integer.parseInt(cmd.getOptionValue("p", "55555"));

		// initializing the Socket Server
		Server server = new Server(portNumber);
		server.run();  	
		
		log.info("Server is shutdown");

    }
}
