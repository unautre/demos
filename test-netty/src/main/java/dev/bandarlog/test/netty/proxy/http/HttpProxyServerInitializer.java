package dev.bandarlog.test.netty.proxy.http;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;

public class HttpProxyServerInitializer extends ChannelInitializer<SocketChannel> {

	private final Channel inboundChannel;

	public HttpProxyServerInitializer(Channel inboundChannel) {
		this.inboundChannel = inboundChannel;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast( //
				new HttpResponseDecoder(), //
				new HttpRequestEncoder(), //
				new HttpProxyBackendHandler(inboundChannel) //
		);
	}
}
