package dev.bandarlog.www.test.bsky;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.util.ReferenceCountUtil;

public class NettyMain {

	public static void main(String[] args) throws Exception {

		final URI uri = new URI("wss://bsky.network/xrpc/com.atproto.sync.subscribeRepos");
		final String host = uri.getHost();
		int port = uri.getPort() != -1 ? uri.getPort() : uri.getScheme().equals("wss") ? 443 : 80;

		final InetSocketAddress proxy = null;

		// Configure the client.
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
					.handler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) throws Exception {
							final ChannelPipeline p = ch.pipeline();

							if (proxy != null) {
								p.addLast(new HttpProxyHandler(proxy));
							}

							final SslContext sslCtx = SslContextBuilder.forClient().build();
							p.addLast(sslCtx.newHandler(ch.alloc()));

							p.addLast(new HttpClientCodec());
							p.addLast(new HttpObjectAggregator(1024 * 1024));

							p.addLast(new WebSocketClientProtocolHandler(uri, WebSocketVersion.V13, null, false,
									EmptyHttpHeaders.INSTANCE, 1024 * 1024));
							p.addLast(WebSocketClientCompressionHandler.INSTANCE);

							p.addLast(new LoggingHandler("FRAME LEVEL"));
							p.addLast(new BskyHandler());
						}
					});

			// Start the client.
			ChannelFuture f = b.connect(host, port).sync();

			// Wait until the connection is closed.
			f.channel().closeFuture().sync();
		} finally {
			// Shut down the event loop to terminate all threads.
			group.shutdownGracefully();
		}
	}

	public static class BskyHandler extends ChannelInboundHandlerAdapter {

		private final CBORMapper cbor = new CBORMapper();

		private final ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

		private volatile int count = 0;

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			System.out.println("#channelRead(ctx, " + msg + ");");

			if (msg instanceof BinaryWebSocketFrame frame) {
				try (final InputStream input = new ByteBufInputStream(frame.content())) {
					final MappingIterator<Map<String, Object>> it = cbor.readerFor(Map.class).readValues(input);

					final Map<String, Object> header = it.next();
					final Map<String, Object> payload = it.next();

					System.out.println(json.writeValueAsString(header));
					System.out.println(json.writeValueAsString(payload));

					byte[] blocks = (byte[]) payload.get("blocks");
					if (blocks != null) {
						final ByteBuf buf = Unpooled.wrappedBuffer(blocks);

//						System.out.println(ByteBufUtil.prettyHexDump(buf));

						{ // CAR header
							final long headerSize = readVarint(buf);
							final ByteBuf headerContent = buf.readRetainedSlice((int) headerSize);
							try (final InputStream headerContentStream = new ByteBufInputStream(headerContent)) {
								final JsonNode tree = cbor.readerFor(Map.class).readTree(headerContentStream);
								System.out.println("Car header: " + tree);
							}
						}

						while (buf.isReadable()) {
							/**
							 * https://ipld.io/specs/transport/car/carv1/
							 */

							final long sectionSize = readVarint(buf);
							final ByteBuf section = buf.readRetainedSlice((int) sectionSize);

							// read CID
							/**
							 * https://github.com/multiformats/cid
							 */
							final int cidStart = section.readerIndex();

							final int cidVersion = (int) readVarint(section);
							final long contentType = readVarint(section);

							System.out.println("\tversion: " + cidVersion + " contentType: 0x" + Long.toHexString(contentType));

							{ // read multihash
								final long function = readVarint(section);
								final long size = readVarint(section);
								
								final ByteBuf hash = section.readRetainedSlice((int) size);

								System.out.println("\tCID: function:" + function + " size:" + size + " hash:" + ByteBufUtil.hexDump(hash));
							}
							
							System.out.println(Base64.encode(section, cidStart, section.readerIndex()).toString(StandardCharsets.UTF_8));

							if (contentType == 0x71) {
								try (final InputStream payloadInput = new ByteBufInputStream(section)) {
									final JsonNode sectionData = cbor.readerFor(Map.class).readTree(payloadInput);
									
									
									
									System.out.println("\t" + sectionData);
								}
							} else {
								System.out.println("Unsupported content type 0x" + Long.toHexString(contentType));
								System.out.println(ByteBufUtil.prettyHexDump(section, section.readerIndex(), section.readableBytes()));
							}
						}
					}

				} finally {
					ReferenceCountUtil.release(msg);

					if (count++ == 3) {
						System.exit(1);
					}
				}
			}
		}

		/**
		 * https://en.wikipedia.org/wiki/LEB128
		 */
		public static long readVarint(ByteBuf buf) {
			long result = 0;
			long shift = 0;
			while (true) {
				byte b = buf.readByte();
				result |= (b & 0b1111111) << shift;
				if ((b & 0b10000000) == 0)
					break;
				shift += 7;
			}
			return result;
		}
	}
	
	public static class CID {
		
	}
	
	public static class Node {
		@JsonProperty("l") public String left;
		@JsonProperty("e") public List<TreeEntry> entries;
	}
	
	public static class TreeEntry {
		@JsonProperty("p") public int prefixlen;
		@JsonProperty("k") public String keysuffix;
		@JsonProperty("v") public String value;
	}
	
	public static class MultiCodec {
		public static final long IDENTITY = 0x0;
		public static final long CIDv1 = 0x1;
		public static final long CIDv2 = 0x2;
		public static final long CIDv3 = 0x3;
		
		public static final long SHA1 = 0x11;
		public static final long SHA2_256 = 0x12;
		
		public static final long MULTICODEC = 0x30;
		public static final long MULTIHASH = 0x31;
		
		public static final long SHAKE_128 = 0x18;
		public static final long DAG_CBOR = 0x71;
	}
}