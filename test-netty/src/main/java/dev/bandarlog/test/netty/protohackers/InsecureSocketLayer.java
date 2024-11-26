package dev.bandarlog.test.netty.protohackers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.bandarlog.test.netty.protohackers.InsecureSocketLayer.Cipher.CipherList;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

public class InsecureSocketLayer {

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
									new CipherReceiver(), //
									new ObfuscationLayer(), //
									new LoggingHandler(LogLevel.INFO), //
									new LineBasedFrameDecoder(0xFFFF), //
									new LineCodec(), //
									new LoggingHandler(LogLevel.INFO), //
									new ApplicationLayer());
						};
					}); //

			b.bind(LOCAL_PORT).sync().channel().closeFuture().sync();
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	public interface Cipher {

		byte encode(byte value, int position);
		
		default byte decode(byte value, int position) {
			return encode(value, position);
		}

		public static final Cipher INVERT_BITS = (b, pos) -> {
			int i = b;
			i = (i & 0x55555555) << 1 | (i >>> 1) & 0x55555555;
			i = (i & 0x33333333) << 2 | (i >>> 2) & 0x33333333;
			i = (i & 0x0f0f0f0f) << 4 | (i >>> 4) & 0x0f0f0f0f;

			return (byte) (i & 0xFF);
		};

		public interface Xor extends Cipher {
			public static Xor of(byte code) {
				return (b, pos) -> (byte) (b ^ code);
			}
		}

		public static final Cipher XOR_POS = (b, pos) -> (byte) (b ^ pos);

		public class Add implements Cipher {
			private final byte code;
			public Add(byte code) { this.code = code; }
			
			public byte encode(byte b, int pos) { return (byte) ((b + code) & 0xFF); }
			public byte decode(byte b, int pos) { return (byte) ((b - code) & 0xFF); }
		}

		public static final Cipher ADD_POS = new Cipher() {
			public byte encode(byte b, int pos) { return (byte) ((b + pos) & 0xFF); }
			public byte decode(byte b, int pos) { return (byte) ((b - pos) & 0xFF); }
		};
		
		public static class CipherList extends ArrayList<Cipher> implements Cipher {
			
			@Override
			public byte encode(byte value, int position) {
				for (int i = 0 ; i < size(); i++) {
					value = get(i).encode(value, position);
				}
				return value;
			}
			
			@Override
			public byte decode(byte value, int position) {
				for (int i = size() - 1; i >= 0; i--) {
					value = get(i).decode(value, position);
				}
				return value;
			}
			
			public boolean isValid() {
				for (int i = 0; i < 256; i++) {
					final byte b = (byte) (i & 0xFF);
					
					if (encode(b, 42) != b)
						return true;
					
					if (encode(b, 43) != b)
						return true;
				}
				
				return false;
			}
		}
	}

	public static class CipherReceiver extends ByteToMessageDecoder {

		private final CipherList ciphers = new CipherList();

		@Override
		protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

			in.markReaderIndex();
			final byte peek = in.readByte();

			if (peek == 2 || peek == 4) {
				if (in.readableBytes() <= 0) {
					in.resetReaderIndex();
					return;
				}
			}

			switch (peek) {
			case 0:
				// end of cipher list.
				ctx.pipeline().remove(this);

				if (!ciphers.isValid()) {
					ctx.close();
					return;
				}
				out.add(ciphers);
				break;
			case 1:
				ciphers.add(Cipher.INVERT_BITS);
				break;
			case 2:
				ciphers.add(Cipher.Xor.of(in.readByte()));
				break;
			case 3:
				ciphers.add(Cipher.XOR_POS);
				break;
			case 4:
				ciphers.add(new Cipher.Add(in.readByte()));
				break;
			case 5:
				ciphers.add(Cipher.ADD_POS);
				break;
			}
		}
	}

	public static class ObfuscationLayer extends ChannelDuplexHandler {

		private Cipher cipher;

		private int in = 0, out = 0;

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (msg instanceof Cipher) {
				// install ciphers
				this.cipher = (Cipher) msg;
			} else if (msg instanceof ByteBuf) {
				final ByteBuf message = (ByteBuf) msg;

				for (int i = message.readerIndex(); i < message.writerIndex(); i++) {
					final Byte ciphered = cipher.decode(message.getByte(i), in++);
					message.setByte(i, ciphered);
				}

				super.channelRead(ctx, message);
			}
		}

		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			if (msg instanceof ByteBuf) {
				final ByteBuf message = (ByteBuf) msg;

				for (int i = message.readerIndex(); i < message.writerIndex(); i++) {
					final Byte ciphered = cipher.encode(message.getByte(i), out++);
					message.setByte(i, ciphered);
				}
			}

			super.write(ctx, msg, promise);
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

	public static class ApplicationLayer extends SimpleChannelInboundHandler<String> {

		@Override
		protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
			final String[] splat = msg.split(",");

			final String max = Arrays.stream(splat).max(Comparator.comparing((String s) -> {
				final Matcher m = Pattern.compile("(\\d+)").matcher(s);
				m.find();
				final String group = m.group(1);
				return Integer.valueOf(group);
			})).get();

			ctx.writeAndFlush(max);
		}
	}
}