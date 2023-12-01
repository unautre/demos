package dev.bandarlog.test.netty.proxy.postgres;

import java.util.Map.Entry;

import dev.bandarlog.test.netty.proxy.postgres.PostgresMessages.RequestMessages.SSLNegociationMessage;
import dev.bandarlog.test.netty.proxy.postgres.PostgresMessages.RequestMessages.StartupMessage;
import dev.bandarlog.test.netty.proxy.postgres.PostgresMessages.ResponseMessages.NoSSLResponseMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class PostgresServerMock extends SimpleChannelInboundHandler<PostgresMessages> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, PostgresMessages msg) throws Exception {
		if (msg instanceof SSLNegociationMessage) {
			ctx.writeAndFlush(new NoSSLResponseMessage());
		} else if (msg instanceof StartupMessage) {
			final StartupMessage startup = (StartupMessage) msg;
			
			System.out.println("Receiving connection:");
			for (Entry<String, String> entry : startup.payload.entrySet()) {
				System.out.println("\t" + entry);
			}
		}
	}
}