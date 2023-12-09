package dev.bandarlog.test.netty.proxy.postgres.lean;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.Password;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.Query;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.StartupMessage;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.Sync;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.Terminate;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class PostgresRequestDecoder extends ByteToMessageDecoder {

	private static final Logger LOGGER = LoggerFactory.getLogger(PostgresRequestDecoder.class);

	private boolean waitingForStartup = true;

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (waitingForStartup) {
			if (in.readableBytes() < 8)
				return;

			final int totalSize = in.getInt(in.readerIndex());

			if (in.readableBytes() < totalSize)
				return;

			// create subframe dedicated for this message
			in = in.readRetainedSlice(totalSize);

			final int protocolVersion = in.getInt(4);

			final StartupMessage msg = new StartupMessage(in);

			if (protocolVersion != StartupMessage.SSL_NEGOCIATION_PROTOCOL_VERSION) {
				waitingForStartup = false;
			}

			out.add(msg);
		} else {
			// all other messages begin with a letter, and a size (32bit).
			if (in.readableBytes() < 5)
				return;

			final byte name = in.getByte(in.readerIndex());
			final int totalSize = in.getInt(in.readerIndex() + 1);

			if (in.readableBytes() < totalSize) {
				return;
			}
			
			in = in.readRetainedSlice(totalSize);

			final PostgresMessages msg;
			switch (name) {
			case 'Q':
				msg = new Query(in);
				break;
			case 'p':
				msg = new Password(in);
				break;
			case 'X':
				msg = new Terminate(in);
				break;
			case 'S':
				msg = new Sync(in);
				break;
			default:
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Unhandled message type {}: passing generic message", name);
				}
				msg = new PostgresMessages(in);
			}

			out.add(msg);
		}
	}
}