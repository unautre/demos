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
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * This class here implements the MITM logic:
 * <ul>
 * <li>Forces a ssl-less connection</li>
 * </ul>
 */
public class SSLStripper extends ChannelDuplexHandler {

	private static final ChannelFutureListener FIRE = ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
	
	private static final ByteBuf N = Unpooled.wrappedBuffer(new byte[] { 'N' });
	
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
			}
		}
		
		super.channelRead(ctx, msg);
	}
}