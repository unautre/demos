package dev.bandarlog.test.netty.proxy.postgres.lean.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class PostgresEncoder extends ChannelOutboundHandlerAdapter {

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		if (msg instanceof PostgresMessages) {
			final PostgresMessages pgMsg = (PostgresMessages) msg;
			final ByteBuf buf = pgMsg.content();
			buf.readerIndex(0);
			
			msg = buf;
		}
		super.write(ctx, msg, promise);
	}
}