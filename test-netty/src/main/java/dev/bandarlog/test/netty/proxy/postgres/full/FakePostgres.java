package dev.bandarlog.test.netty.proxy.postgres.full;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public final class FakePostgres {

	static final int LOCAL_PORT = Integer.parseInt(System.getProperty("localPort", "5432"));

	public static void main(String[] args) throws Exception {
		// Configure the bootstrap.
		final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		final EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			final ServerBootstrap b = new ServerBootstrap() //
					.group(bossGroup, workerGroup) //
					.channel(NioServerSocketChannel.class) //
					.handler(new LoggingHandler(LogLevel.TRACE)) //
					.childHandler(new FakePostgresInitializer()) //
//					.childOption(ChannelOption.AUTO_READ, false) //
					;
			
			b.bind(LOCAL_PORT).sync().channel().closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static class FakePostgresInitializer extends ChannelInitializer<SocketChannel> {

		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			ch.pipeline().addLast( //
					new LoggingHandler(LogLevel.ERROR), //
					new PostgresResponseEncoder(), //
					new PostgresRequestDecoder(), //
					new PostgresServerMock() //
			);
		}
	}
}