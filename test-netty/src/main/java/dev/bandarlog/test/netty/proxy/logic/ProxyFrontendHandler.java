package dev.bandarlog.test.netty.proxy.logic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.SocketChannel;

public class ProxyFrontendHandler extends ChannelInboundHandlerAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProxyFrontendHandler.class);
	
	private final String remoteHost;
	private final int remotePort;

	// As we use inboundChannel.eventLoop() when building the Bootstrap this does
	// not need to be volatile as
	// the outboundChannel will use the same EventLoop (and therefore Thread) as the
	// inboundChannel.
	private Channel outboundChannel;
	
	private ChannelHandler delegate;

	public ProxyFrontendHandler(String remoteHost, int remotePort, ChannelHandler delegate) {
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
		this.delegate = delegate;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		final Channel inboundChannel = ctx.channel();

		// Start the connection attempt.
		final Bootstrap b = new Bootstrap() //
				.group(inboundChannel.eventLoop()) //
				.channel(ctx.channel().getClass()) //
				.handler(new ProxyServerInitializer(delegate, inboundChannel)) //
				.option(ChannelOption.AUTO_READ, false);
		final ChannelFuture f = b.connect(remoteHost, remotePort);
		outboundChannel = f.channel();
		f.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) {
				if (future.isSuccess()) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Connected {} to {}", inboundChannel, outboundChannel);
					}
					// connection complete start to read first data
					inboundChannel.read();
				} else {
					if (LOGGER.isErrorEnabled()) {
						LOGGER.error("Could not connect to proxy target {}:{}", remoteHost, remotePort, future.cause());
					}
					// Close the connection if the connection attempt has failed.
					inboundChannel.close();
				}
			}
		});
	}

	@Override
	public void channelRead(final ChannelHandlerContext ctx, Object msg) {
		if (outboundChannel.isActive()) {
			outboundChannel.writeAndFlush(msg).addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) {
					if (future.isSuccess()) {
						// was able to flush out data, start to read the next chunk
						ctx.channel().read();
					} else {
						future.channel().close();
					}
				}
			});
		}
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		if (outboundChannel != null) {
			closeOnFlush(outboundChannel);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		closeOnFlush(ctx.channel());
	}

	/**
	 * Closes the specified channel after all queued write requests are flushed.
	 */
	static void closeOnFlush(Channel ch) {
		if (ch.isActive()) {
			ch.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}
}