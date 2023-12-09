package dev.bandarlog.test.netty.proxy.postgres.lean;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

public class LoginStealer extends ChannelDuplexHandler {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("Reading: " + msg);
		super.channelRead(ctx, msg);
	}
	
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		System.out.println("Writing:" + msg);
		super.write(ctx, msg, promise);
	}
}