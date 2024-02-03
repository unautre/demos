package dev.bandarlog.test.netty.proxy.postgres.lean.auth;

import dev.bandarlog.test.netty.proxy.postgres.lean.codec.PostgresMessages.StartupMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

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
				
				ctx.writeAndFlush(N.retain()).addListener(FIRE);
				ctx.read();
				
				return;
			}
		}
		
		super.channelRead(ctx, msg);
	}
}