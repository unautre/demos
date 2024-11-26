package dev.bandarlog.test.netty.proxy.postgres.full;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;

import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.RequestMessages.CancelRequestMessage;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.RequestMessages.PasswordMessage;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.RequestMessages.Query;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.RequestMessages.SSLNegociationMessage;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.RequestMessages.StartupMessage;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.RequestMessages.Sync;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.RequestMessages.Terminate;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class PostgresRequestDecoder extends ByteToMessageDecoder {

	private static final Charset CHARSET = StandardCharsets.US_ASCII;
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
			in = in.readSlice(totalSize);

			// skip size previously readen
			in.readInt();

			final int protocolVersion = in.readInt();

			if (protocolVersion == CancelRequestMessage.CANCEL_REQUEST_PROTOCOL_VERSION) {
				final CancelRequestMessage msg = new CancelRequestMessage();

				msg.processId = in.readInt();
				msg.secretKey = in.readInt();

				out.add(msg);
			} else if (protocolVersion == SSLNegociationMessage.SSL_NEGOCIATION_PROTOCOL_VERSION) {
				out.add(new SSLNegociationMessage());
			} else {
				waitingForStartup = false;

				final StartupMessage msg = new StartupMessage();
				msg.version = protocolVersion;
				msg.payload = new LinkedHashMap<>();

				// read headers
				// null terminator is expected at the end
				while (in.readableBytes() > 1) {
					final String key = readCString(in);
					final String value = readCString(in);

					msg.payload.put(key, value);
				}

				out.add(msg);
			}
		} else {
			// all other messages begin with a letter, and a size (32bit).
			if (in.readableBytes() < 5)
				return;

			in.markReaderIndex();

			final byte name = in.readByte();

			final int totalSize = in.readInt();

			if (in.readableBytes() + 5 < totalSize) {
				in.resetReaderIndex();
				return;
			}

			switch (name) {
			case 'Q':
				final Query query = new Query();
				query.query = readCString(in);
				out.add(query);
				break;
			case 'p':
				final PasswordMessage pass = new PasswordMessage();
				pass.password = readCString(in);
				out.add(pass);
				break;
			case 'X':
				final Terminate terminate = new Terminate();
				out.add(terminate);
				break;
			case 'S':
				final Sync sync = new Sync();
				out.add(sync);
				break;
			}
		}
	}

	static String readCString(ByteBuf in) {
		final int nullTerminator = in.bytesBefore((byte) 0);

		final CharSequence seq;
		if (nullTerminator != -1) {
			seq = in.readCharSequence(nullTerminator, CHARSET);

			// skip the \0
			in.skipBytes(1);
		} else {
			seq = in.readCharSequence(in.readableBytes(), CHARSET);
		}

		return seq.toString();
	}
}