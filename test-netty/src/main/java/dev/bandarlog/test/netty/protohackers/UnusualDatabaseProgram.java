package dev.bandarlog.test.netty.protohackers;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;

public class UnusualDatabaseProgram {

	static final int LOCAL_PORT = Integer.parseInt(System.getProperty("localPort", "1337"));

	public static void main(String[] args) throws Exception {

		ResourceLeakDetector.setLevel(Level.PARANOID);
		
		// Configure the bootstrap.
		final EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			final Bootstrap b = new Bootstrap() //
				.group(workerGroup) //
				.channel(NioDatagramChannel.class) //
				.handler(new ChannelInitializer<Channel>() {
					protected void initChannel(Channel ch) throws Exception {
						ch.pipeline().addLast( //
							new LoggingHandler(LogLevel.INFO), //
							new DBHandler() //
						);
					};
				}); //

			b.bind(LOCAL_PORT).sync().channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
		}
	}
	
	public static class DBHandler extends SimpleChannelInboundHandler<DatagramPacket> {
		
		private static final ByteBuf VERSION_ANSWER = Unpooled.copiedBuffer("version=Poor man's redis v0.1", StandardCharsets.US_ASCII);
		
		private static final ByteBuf VERSION_KEY = Unpooled.copiedBuffer("version", StandardCharsets.US_ASCII);
		
		private static final Map<ByteBuf, ByteBuf> DATA = new ConcurrentHashMap<>();
		
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception {
			final ByteBuf buf = msg.content();
			
			final int index = buf.bytesBefore((byte) '=');
			
			if (index == -1) {
				// read

				final ByteBuf answer;
				
				if (VERSION_KEY.equals(buf)) {
					answer = VERSION_ANSWER;
				} else {
					answer = DATA.get(buf);
				}
				
				if (answer != null) {
					final DatagramPacket packet = new DatagramPacket(answer.retain().slice(), msg.sender());
					
					ctx.writeAndFlush(packet);
				}
			} else {
				// write
				buf.markReaderIndex();
				final ByteBuf key = buf.readRetainedSlice(index);
				buf.resetReaderIndex();
				
				buf.retain();
				
				final ByteBuf previous = DATA.put(key, buf);
				
				if (previous != null) {
					previous.release();
				}
			}
		}
	}
}