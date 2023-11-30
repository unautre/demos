package dev.bandarlog.test.netty.proxy.hexdump;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * Change case from uppercase (client) to lowercase (server)
 */
public class CaseChangerHandler extends ChannelDuplexHandler {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		final ByteBuf buf = (ByteBuf) msg;

		System.out.println("read: " + buf);
		
		for (int i = buf.readerIndex(); i < buf.writerIndex(); i++) {		
			final byte b = buf.getByte(i);

			buf.setByte(i, b ^ 32);
		}

		super.channelRead(ctx, msg);
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		final ByteBuf buf = (ByteBuf) msg;
		
		System.out.println("write: " + buf);

		for (int i = buf.readerIndex(); i < buf.writerIndex(); i++) {
			final byte b = buf.getByte(i);

			buf.setByte(i, b ^ 32);
		}

		super.write(ctx, msg, promise);
	}
}