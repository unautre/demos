package dev.bandarlog.test.netty.proxy.postgres.lean;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.AuthenticationResponse;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.BackendKeyData;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.ParameterStatus;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.ReadyForQuery;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class PostgresResponseDecoder extends ByteToMessageDecoder {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(PostgresResponseDecoder.class);

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		
		// TODO: SSL negociation response
		// TODO: all payload-less messages which are just a byte
		// state machine will be required
		
		final byte name = in.getByte(in.readerIndex());
		
		if (in.readableBytes() < 5) {
			return;
		}
		
		final int totalSize = in.getInt(in.readerIndex() + 1);
		
		if (in.readableBytes() < totalSize + 1) {
			return;
		}
		
		in = in.readRetainedSlice(totalSize + 1);
		
		final PostgresMessages msg;
		switch (name) {
		case 'R':
			msg = new AuthenticationResponse(in);
			break;
		case 'S':
			msg = new ParameterStatus(in);
			break;
		case 'K':
			msg = new BackendKeyData(in);
			break;
		case 'Z':
			msg = new ReadyForQuery(in);
			break;
		default:
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Unhandled message type {}: passing generic message", Character.toString(name));
			}
			msg = new PostgresMessages(in);
			break;
		}
		
		out.add(msg);
	}
}