package dev.bandarlog.test.netty.proxy.postgres.lean;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class PostgresEncoder extends ChannelOutboundHandlerAdapter {

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		System.out.println("Asked to encode: " + msg);
		if (msg instanceof PostgresMessages) {
			msg = ((PostgresMessages) msg).content();
		}
		super.write(ctx, msg, promise);
	}
}