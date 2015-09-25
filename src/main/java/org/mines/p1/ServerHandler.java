/*
 * Course: CSCI-565-Distributed Computing Systems
 *
 * Student: Henri van den Bulk
 */
package org.mines.p1;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles both client-side and server-side handler depending on which
 * constructor was called.
 */
@Slf4j
public class ServerHandler extends SimpleChannelInboundHandler<Object> {

	private static final ChannelGroup channels = new DefaultChannelGroup(null);
	private static final ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();

	private final int sessionId = this.hashCode();

	@Override
	protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {

		log.info("Got a messageReceived: ");

	}

	/**
	 * Making reader for when data is available in the channel.
	 */
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		boolean close = false;
		long id = (int) (Math.random() * 10000);
		Message request = (Message) msg;
		Command cmd = request.getCommand();
		Message response = null;

		log.debug(">> [{}]", request);
		HashMap<String, String> headers = new HashMap<String, String>();

		switch (cmd) {
		case SEND:
			log.debug("SEND Command");
			// String destination = (String) h.get("destination");
			headers.put("receipt-id", "message-" + String.valueOf(id));

			response = new Message(Command.RECEIPT, headers, "Got it");

			queue.offer(request.getBody());
			log.info("Message added to the queue");

			break;
		case CONNECT:
			headers.put("session", String.valueOf(sessionId));
			response = new Message(Command.CONNECTED, headers, "Got it");

			break;
		case SUBSCRIBE:
			// The client indicates they want to subscribe to a queue. We'll
			// provide the oldest item in the queue. If we don't have any
			// items in the queue, we'll provide an empty message back to the 
			// client.
			if (!queue.isEmpty()) {
				response = new Message(Command.MESSAGE, headers, queue.poll());
			}
			else {
				response = new Message(Command.MESSAGE, headers, null);
			}
			break;
		case DISCONNECT:
			headers.put("session", String.valueOf(sessionId));
			response = new Message(Command.RECEIPT, headers, null);
			close = true;
			break;
		}

		// Only send something back if we have a response to send back
		if (response != null) {
			log.debug("Response Message [{}]", response);
			ChannelFuture future = ctx.writeAndFlush(response);

			if (close) {
				future.addListener(ChannelFutureListener.CLOSE);
			}
		}
	}

	/**
	 * Called when a new client has joined.
	 */
	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		// Add the channels to the list of channels, incase we want to talk to
		// the others
		channels.add(ctx.channel());
		log.info("Added client [{}] sesion [{}]", ctx.channel().remoteAddress(), String.valueOf(sessionId));

	};

	@Override
	public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
		channels.remove(ctx.channel());
		log.info("Removed client [{}] sesion [{}]", ctx.channel().remoteAddress(), String.valueOf(sessionId));
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

}
