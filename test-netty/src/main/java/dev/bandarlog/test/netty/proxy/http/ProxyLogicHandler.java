package dev.bandarlog.test.netty.proxy.http;

import java.nio.charset.StandardCharsets;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.QueryStringDecoder;

public class ProxyLogicHandler extends ChannelDuplexHandler {
	
	private boolean dump = false;
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		
		if (msg instanceof HttpRequest) {
			final HttpRequest req = (HttpRequest) msg;
			
			System.out.printf(">>> %s %s %s%n", req.method(), req.uri(), req.protocolVersion());
			System.out.println(">>> " + req.headers());
			
			final QueryStringDecoder queryDecoder = new QueryStringDecoder(req.uri());
			
			dump = (queryDecoder.parameters().get("partNumber") == null);
		} else if (msg instanceof HttpContent && dump) {
			System.out.println("> " + msg);
		}
		
		super.channelRead(ctx, msg);
	}
	
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		
		if (msg instanceof HttpResponse) {
			final HttpResponse resp = (HttpResponse) msg;
			
			System.out.printf("<<< %d %s%n", resp.status().code(), resp.status().reasonPhrase());
		} else if (msg instanceof HttpContent) {
			final HttpContent content = (HttpContent) msg;
			
			System.out.println("< " + content.content().toString(StandardCharsets.UTF_8).replaceAll("\n(.)", "\n< $1"));
		}
		
		super.write(ctx, msg, promise);
	}
}