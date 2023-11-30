package dev.bandarlog.test.netty.proxy.http;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

public class HttpProxyClientInitializer extends ChannelInitializer<SocketChannel> {

	private final String remoteHost;
	private final int remotePort;

	public HttpProxyClientInitializer(String remoteHost, int remotePort) {
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
	}

	@Override
	public void initChannel(SocketChannel ch) {
		ch.pipeline().addLast( //
				new HttpRequestDecoder(), //
//				new HttpObjectAggregator(65536), //
				new HttpResponseEncoder(), //
				new ProxyLogicHandler(), //
				new ProxyFrontendHandler(remoteHost, remotePort) //
		);
	}
}