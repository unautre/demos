package dev.bandarlog.test.netty.proxy.http;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;

public class ProxyLogicHandler extends ChannelDuplexHandler {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		
		if (msg instanceof HttpRequest) {
			final HttpRequest req = (HttpRequest) msg;
			
			System.out.printf(">>> %s %s %s%n", req.method(), req.uri(), req.protocolVersion());
		}
		
		super.channelRead(ctx, msg);
	}
	
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		
		if (msg instanceof HttpResponse) {
			final HttpResponse resp = (HttpResponse) msg;
			
			System.out.printf("<<< %d %s%n", resp.status().code(), resp.status().codeAsText());
		}
		
		super.write(ctx, msg, promise);
	}
}