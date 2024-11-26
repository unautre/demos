package dev.bandarlog.test.netty.proxy.http;

import dev.bandarlog.test.netty.proxy.logic.ProxyFrontendHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.incubator.channel.uring.IOUringEventLoopGroup;
import io.netty.incubator.channel.uring.IOUringServerSocketChannel;

public final class HttpDumpProxy {

	static final int LOCAL_PORT = Integer.parseInt(System.getProperty("localPort", "8080"));
	static final String REMOTE_HOST = System.getProperty("remoteHost", "localhost");
	static final int REMOTE_PORT = Integer.parseInt(System.getProperty("remotePort", "9000"));

	public enum ChannelTypeEnum {
		IO_URING, EPOLL, NIO;
	}
	
	public static void main(String[] args) throws Exception {
		System.err.println("Proxying *:" + LOCAL_PORT + " to " + REMOTE_HOST + ':' + REMOTE_PORT + " ...");

		// Configure the bootstrap.
		final ChannelTypeEnum channelType = ChannelTypeEnum.IO_URING;
		
		final EventLoopGroup bossGroup;
		final EventLoopGroup workerGroup;
		final Class<? extends ServerChannel> channel;
		
		if (channelType == ChannelTypeEnum.IO_URING) {
			bossGroup = new IOUringEventLoopGroup(1);
			workerGroup = new IOUringEventLoopGroup();
			channel = IOUringServerSocketChannel.class;
		} else if (channelType == ChannelTypeEnum.EPOLL) {
			bossGroup = new EpollEventLoopGroup(1);
			workerGroup = new EpollEventLoopGroup();
			channel = EpollServerSocketChannel.class;
		} else if (channelType == ChannelTypeEnum.NIO) {
			bossGroup = new NioEventLoopGroup(1);
			workerGroup = new NioEventLoopGroup();
			channel = NioServerSocketChannel.class;
		} else {
			throw new IllegalArgumentException("Unexpected channel type " + channelType);
		}
		
		try {
			ServerBootstrap b = new ServerBootstrap() //
					.group(bossGroup, workerGroup) //
					.channel(channel) //
					.childHandler(new HttpProxyClientInitializer(REMOTE_HOST, REMOTE_PORT)) //
					.childOption(ChannelOption.AUTO_READ, false);
			b.bind(LOCAL_PORT).sync().channel().closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
	
	public static class HttpProxyClientInitializer extends ChannelInitializer<SocketChannel> {

		private final String remoteHost;
		private final int remotePort;

		public HttpProxyClientInitializer(String remoteHost, int remotePort) {
			this.remoteHost = remoteHost;
			this.remotePort = remotePort;
		}

		@Override
		public void initChannel(SocketChannel ch) {
			ch.pipeline().addLast( //
					new HttpServerCodec(),
//					new HttpRequestDecoder(), //
//					new HttpObjectAggregator(65536), //
//					new HttpResponseEncoder(), //
					new ProxyLogicHandler(), //
					new ProxyFrontendHandler(remoteHost, remotePort, new HttpProxyServerInitializer()) //
			);
		}
	}
	
	public static class HttpProxyServerInitializer extends ChannelInitializer<SocketChannel> {

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ch.pipeline().addLast( //
//					new HttpResponseDecoder(), //
//					new HttpRequestEncoder() //
					new HttpClientCodec()
			);
		}
	}
}