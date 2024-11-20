package dev.bandarlog.www.test.bsky;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBufInputStream;
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
import io.netty.handler.codec.http.EmptyHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.logging.LogLevel;
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
            b.group(group)
             .channel(NioSocketChannel.class)
             .option(ChannelOption.TCP_NODELAY, true)
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
                     
                     p.addLast(new WebSocketClientProtocolHandler(uri, WebSocketVersion.V13, null, false, EmptyHttpHeaders.INSTANCE, 1024 * 1024));
                     p.addLast(WebSocketClientCompressionHandler.INSTANCE);
                     
//                     p.addLast(new LoggingHandler(LogLevel.INFO));
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
    	
    	private final CBORMapper mapper = new CBORMapper();
    	
    	private volatile int count = 0;
    	
    	@Override
    	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    		System.out.println("#channelRead(ctx, " + msg + ");"); count++;
    		
    		if (msg instanceof BinaryWebSocketFrame frame) {
    			try (final InputStream input = new ByteBufInputStream(frame.content())) {
    				final MappingIterator<Object> it = mapper.readerFor(Map.class).readValues(input);
    				
    				while (it.hasNext()) {
    					System.out.println(it.next());
    				}
    			} finally {
    				ReferenceCountUtil.release(msg);
    			}
    		}
    		
    		if (count == 10) {
    			System.exit(1);
    		}
    	}
    }
}