package dev.bandarlog.test.netty.proxy.postgres;

import dev.bandarlog.test.netty.proxy.postgres.PostgresMessages.ResponseMessages;
import dev.bandarlog.test.netty.proxy.postgres.PostgresMessages.ResponseMessages.NoSSLResponseMessage;
import dev.bandarlog.test.netty.proxy.postgres.PostgresMessages.ResponseMessages.SSLResponseMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PostgresResponseEncoder extends MessageToByteEncoder<ResponseMessages> {

	@Override
	protected void encode(ChannelHandlerContext ctx, ResponseMessages msg, ByteBuf out) throws Exception {
		if (msg instanceof NoSSLResponseMessage) {
			out.writeByte('N');
		} else if (msg instanceof SSLResponseMessage) {
			out.writeByte('S');
		}
	}
}