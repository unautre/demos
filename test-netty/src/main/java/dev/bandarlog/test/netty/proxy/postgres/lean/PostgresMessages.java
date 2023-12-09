package dev.bandarlog.test.netty.proxy.postgres.lean;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;

public class PostgresMessages extends DefaultByteBufHolder {
	
	private static final int PAYLOAD_START = 5;

	private static final Charset CHARSET = StandardCharsets.US_ASCII;

	public PostgresMessages(ByteBuf data) {
		super(data);
	}

	public static class StartupMessage extends PostgresMessages {

		public static final int SSL_NEGOCIATION_PROTOCOL_VERSION = 80877103;
		
		public static final int CANCEL_REQUEST_PROTOCOL_VERSION =  80877102;

		public StartupMessage(ByteBuf data) {
			super(data);
		}

		public int getTotalSize() {
			return content().getInt(0);
		}

		public int getProtocolVersion() {
			return content().getInt(4);
		}

		public Iterator<Map.Entry<String, String>> getPayload() {
			return null; // TODO
		}
	}

	public static class Query extends PostgresMessages {

		public Query(ByteBuf data) {
			super(data);
		}

		public String getQuery() {
			final ByteBuf buf = content();
			buf.readerIndex(PAYLOAD_START);
			return readCString(buf);
		}
	}

	public static class Password extends PostgresMessages {

		public Password(ByteBuf data) {
			super(data);
		}

		public String getPassword() {
			final ByteBuf buf = content();
			buf.readerIndex(PAYLOAD_START);
			return readCString(buf);
		}
	}

	public static class Sync extends PostgresMessages {

		public Sync(ByteBuf data) {
			super(data);
		}
	}

	public static class Terminate extends PostgresMessages {

		public Terminate(ByteBuf data) {
			super(data);
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