package dev.bandarlog.test.netty.proxy.logic;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;

public class ProxyServerInitializer extends ChannelInitializer<SocketChannel> {

	private final ChannelHandler delegate;
	
	private final Channel inboundChannel;

	public ProxyServerInitializer(ChannelHandler delegate, Channel inboundChannel) {
		this.delegate = delegate;
		this.inboundChannel = inboundChannel;
	}

	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		ch.pipeline().addLast(delegate);
		ch.pipeline().addLast(new ProxyBackendHandler(inboundChannel));
	}
}