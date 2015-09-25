/*
 * Course: CSCI-565-Distributed Computing Systems
 *
 * Student: Henri van den Bulk
 */
package org.mines.p1;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler implementation for the object echo client.  It initiates the
 * ping-pong traffic between the object echo client and server by sending the
 * first message to the server.
 */
@Slf4j
public class ClientHandler extends SimpleChannelInboundHandler<Object> {

	   private enum ClientState {
	        AUTHENTICATING,
	        AUTHENTICATED,
	        SUBSCRIBED,
	        DISCONNECTING
	    }
	
	private ClientState state;
	private String sessionId;
	
    /**
     * Creates a client-side handler.
     */
    public ClientHandler() {
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

    	state = ClientState.AUTHENTICATING;
    	log.info("Connected to [{}]", ctx.channel().remoteAddress());
    	log.debug("Sending Message ");
    	Message request = new Message(Command.CONNECT,null, "Hello");
    	
        ctx.writeAndFlush(request);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
    	Message receipt = (Message)msg;
    	
    	switch (receipt.getCommand()) {
    	case MESSAGE:
    		if (receipt.getBody() != null) {
    			log.info("Message received: [{}]", receipt.getBody());
    		}
    		else {
    			log.info("Error: No Messages");
    		}
    		// Got the message so let's close shop
    		ctx.close();
    		break;
		case ACK:
			break;
		case BEGIN:
			break;
		case CONNECT:
			break;
		case CONNECTED:
			log.debug("We're connected");
			state = ClientState.AUTHENTICATED;
			break;
		case DISCONNECT:
			break;	
		case ERROR:
			log.warn("Some sort of error " + receipt.getBody());
			ctx.close();
			break;
		case NACK:
			break;
		case RECEIPT:
			log.debug("Server indicated receipt");
			break;
		case SEND:
			break;
		case STOMP:
			break;
		case SUBSCRIBE:
			break;
		case UNKNOWN:
			break;
		case UNSUBSCRIBE:
			break;
		default:
			break;
    	}
    	log.debug("<< [{}]", receipt.toString());
    	
    	
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
    	log.info("Channel Read Complete");
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        log.warn("Communication Exception");
        ctx.close();
    }

	@Override
	protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
    	log.info("<< [{}]", msg.toString());
		
	}
}
