package dev.bandarlog.test.netty.protohackers;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.ChannelMatchers;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;

public class BudgetChat {

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
									new LineBasedFrameDecoder(0xFFFF), //
									new LineCodec(), //
									new ChatroomHandler() //
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

	public static class ChatroomHandler extends ChannelDuplexHandler {

		private static final AttributeKey<String> NAME = AttributeKey.newInstance("NAME");

		private static final ChannelGroup GROUP = new DefaultChannelGroup("chatroom", GlobalEventExecutor.INSTANCE);

		private String name;

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			ctx.writeAndFlush("Welcome to budgetchat! What shall I call you?");
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			if (name != null) {
				final String message = "* " + name + " has left the room.";

				GROUP.writeAndFlush(message, ChannelMatchers.isNot(ctx.channel()), true);
				GROUP.remove(ctx.channel());
			}
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			final String in = (String) msg;

			if (name == null) {
				if (!in.chars().allMatch(Character::isLetterOrDigit)) {
					ctx.writeAndFlush("Invalid name, bozo.").addListener(ChannelFutureListener.CLOSE);

					return;
				}

				// successful join !
				final String welcomeMessage = GROUP.stream() //
						.map(ch -> ch.attr(NAME).get()) //
						.collect(Collectors.joining(", ", "* The room contains: ", "."));
				ctx.writeAndFlush(welcomeMessage);

				// add to the group
				ctx.channel().attr(NAME).set(name = in);
				GROUP.add(ctx.channel());

				// tell everyone
				final String notification = "* " + name + " has entered the room";
				GROUP.writeAndFlush(notification, ChannelMatchers.isNot(ctx.channel()), true);
			} else {
				// broadcast the message
				final String message = "[" + name + "] " + in;

				GROUP.writeAndFlush(message, ChannelMatchers.isNot(ctx.channel()), true);
			}
		}
	}
}