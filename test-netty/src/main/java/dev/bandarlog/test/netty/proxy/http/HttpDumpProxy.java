package dev.bandarlog.test.netty.proxy.http;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
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
}