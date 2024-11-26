package dev.bandarlog.test.netty.proxy.cql;

import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Options;
import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Startup;
import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Supported;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

public class CqlProxyLogicHandler extends ChannelDuplexHandler {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof Startup) {
			final Startup startup = (Startup) msg;
			
			System.out.println("STARTUP: " + startup.payload);
		} else if (msg instanceof Options) {
			System.out.println("OPTIONS");
		} else if (msg instanceof Supported) {
			final Supported supported = (Supported) msg;
			
			System.out.println("SUPPORTED: " + supported.payload);
		}
		
		super.channelRead(ctx, msg);
	}
}