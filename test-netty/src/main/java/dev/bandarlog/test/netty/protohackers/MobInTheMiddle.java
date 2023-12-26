package dev.bandarlog.test.netty.protohackers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Pattern;

import dev.bandarlog.test.netty.proxy.logic.ProxyFrontendHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class MobInTheMiddle {

	static final int LOCAL_PORT = Integer.parseInt(System.getProperty("localPort", "1337"));
	
	static final String REMOTE_HOST = System.getProperty("remoteHost", "chat.protohackers.com");
	static final int REMOTE_PORT = Integer.parseInt(System.getProperty("remotePort", "16963"));

	public static void main(String[] args) throws Exception {

		// Configure the bootstrap.
		final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		final EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			ServerBootstrap b = new ServerBootstrap() //
					.group(bossGroup, workerGroup) //
					.channel(NioServerSocketChannel.class) //
					.handler(new LoggingHandler(LogLevel.INFO)) //
					.childOption(ChannelOption.AUTO_READ, false) //
					.childHandler(new ChannelInitializer<Channel>() {
						protected void initChannel(Channel ch) throws Exception {
							ch.pipeline().addLast( //
									new LoggingHandler(LogLevel.INFO), //
									new LineBasedFrameDecoder(0xFFFF), //
									new LineCodec(), //
									new MITM(), 
									new ProxyFrontendHandler(REMOTE_HOST, REMOTE_PORT, new ChannelInitializer<Channel>() {
										@Override
										protected void initChannel(Channel ch) throws Exception {
											ch.pipeline().addLast( //
												new LineBasedFrameDecoder(0xFFFF), //
												new LineCodec()
											);
										}
									}) //
							);
						};
					}); //

			b.bind(LOCAL_PORT).sync().channel().closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public static class LineCodec extends ByteToMessageCodec<String> {

		@Override
		protected void encode(ChannelHandlerContext ctx, String msg, ByteBuf out) throws Exception {
			out.writeCharSequence(msg, StandardCharsets.US_ASCII);
			out.writeByte('\n');
		}

		@Override
		protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
			final CharSequence charSeq = in.readCharSequence(in.readableBytes(), StandardCharsets.US_ASCII);
			out.add(charSeq.toString());
		}
	}
	
	public static class MITM extends ChannelDuplexHandler {
		
		public static final Pattern PATTERN = Pattern.compile("(?<=^| )(7[a-zA-Z0-9]{25,34})(?=$| )");
		
		private String replace(final String s) {
			return PATTERN.matcher(s).replaceAll("7YWHMfk9JZe0LM0g1ZauHuiSxhI");
		}
		
		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			super.write(ctx, replace((String) msg), promise);
		}
		
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			super.channelRead(ctx, replace((String) msg));
		}
	}
}