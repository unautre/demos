package dev.bandarlog.test.netty.protohackers;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class PrimeTime {

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
									new LineBasedFrameDecoder(1024 * 1024), //
									new LoggingHandler(LogLevel.INFO), //
									new PrimeServerHandler() //
							);
						};
					}); //

			b.bind(LOCAL_PORT).sync().channel().closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	@Sharable
	public static class PrimeServerHandler extends ChannelInboundHandlerAdapter {

		private final ObjectMapper mapper = new ObjectMapper();

		private final ByteBuf ERROR = Unpooled.copiedBuffer("fuck you", StandardCharsets.US_ASCII);

		private boolean checkPrimality(Number number) {
			final double asDouble = number.doubleValue();
			if (Math.round(asDouble) != asDouble)
				return false;
			
			final long asLong = number.longValue();
			if (asLong <= 1)
				return false;

			return IntStream.rangeClosed(2, (int) Math.sqrt(asLong)).noneMatch(n -> (asLong % n == 0));
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) {
			final ByteBuf buf = (ByteBuf) msg;
			final InputStream input = new ByteBufInputStream(buf);

			try {
				final Payload query = mapper.readValue(input, Payload.class);

				// checks
				if (!"isPrime".equals(query.method))
					throw new IllegalArgumentException("not isPrime");

				if (query.number == null)
					throw new IllegalArgumentException("null number");

				final Payload response = new Payload();
				response.method = "isPrime";
				response.prime = checkPrimality((Number) query.number);

				final ByteBuf outBuf = ctx.alloc().buffer();
				final OutputStream output = new ByteBufOutputStream(outBuf);
				mapper.writeValue(output, response);
				outBuf.writeByte('\n');
				ctx.writeAndFlush(outBuf.retain());
			} catch (Exception e) {
				System.err.println(e.getMessage());

				ctx.writeAndFlush(ERROR.retain()).addListener(ChannelFutureListener.CLOSE);
			} finally {
				buf.release();
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			// Close the connection when an exception is raised.
			cause.printStackTrace();
			ctx.close();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Payload {

		public String method;

		@JsonInclude(value = Include.NON_NULL)
		public Object number;

		public boolean prime;
	}
}