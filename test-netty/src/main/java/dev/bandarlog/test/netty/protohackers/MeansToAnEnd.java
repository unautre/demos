package dev.bandarlog.test.netty.protohackers;

import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class MeansToAnEnd {

	static final int LOCAL_PORT = Integer.parseInt(System.getProperty("localPort", "1337"));

	public static void main(String[] args) throws Exception {

		// Configure the bootstrap.
		final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		final EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			ServerBootstrap b = new ServerBootstrap() //
					.group(bossGroup, workerGroup) //
					.channel(NioServerSocketChannel.class) //
					.handler(new LoggingHandler(LogLevel.INFO)) //
//					.childOption(ChannelOption.AUTO_READ, false) //
					.childHandler(new ChannelInitializer<Channel>() {
						protected void initChannel(Channel ch) throws Exception {
							ch.pipeline().addLast( //
									new LoggingHandler(LogLevel.INFO), //
									new PriceCodec(), //
									new PriceServerHandler() //
							);
						};
					}); //

			b.bind(LOCAL_PORT).sync().channel().closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}
	
	public static class PriceCodec extends ByteToMessageDecoder {

		@Override
		protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
			if (in.readableBytes() < 9)
				return;
			
			final Payload payload = new Payload();
			payload.type = in.readByte();
			payload.a = in.readInt();
			payload.b = in.readInt();
			
			out.add(payload);
		}
	}

	public static class PriceServerHandler extends ChannelInboundHandlerAdapter {

		private final SortedMap<Integer, Integer> data = new TreeMap<>();
		
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			final Payload payload = (Payload) msg;

			if (payload.type == 'I') {
				// insert
				data.put(payload.a, payload.b);
			} else if (payload.type == 'Q') {
				final int mean;
				
				if (payload.a > payload.b) {
					mean = 0;
				} else {
					final IntSummaryStatistics stats = data.subMap(payload.a, payload.b+1).values().stream().mapToInt(x -> x).summaryStatistics();
					
					if (stats.getCount() == 0) {
						mean = 0;
					} else {
						mean = (int) (stats.getSum() / stats.getCount());
					}
				}
				
				final ByteBuf buf = ctx.alloc().buffer(4);
				buf.writeInt(mean);
				
				ctx.writeAndFlush(buf.retain());
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			// Close the connection when an exception is raised.
			cause.printStackTrace();
			ctx.close();
		}
	}

	public static class Payload {

		public byte type;
		
		public int a, b;
	}
}