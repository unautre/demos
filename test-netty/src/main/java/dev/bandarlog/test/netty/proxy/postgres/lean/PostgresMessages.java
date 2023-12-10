package dev.bandarlog.test.netty.proxy.postgres.lean;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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

		public Map<String, String> getPayload() {
			final ByteBuf buf = content();
			buf.readerIndex(8);
			
			final Map<String, String> map = new HashMap<>();
			while (buf.isReadable()) {
				final String key = readCString(buf);
				final String value = readCString(buf);
				
				map.put(key, value);
			}
			
			buf.readerIndex(0);
			
			return map;
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
	
	public static class AuthenticationResponse extends PostgresMessages {
		
		public AuthenticationResponse(ByteBuf data) {
			super(data);
		}
		
		enum AuthenticationResponseType {
			OK(0), //
			KERBEROS_V5(2), //
			CLEARTEXT(3), //
			MD5(5), //
			GSS(7), //
			GSS_CONTINUE(8), //
			SSPI(9), //
			SASL(10), //
			SASL_CONTINUE(11), //
			SASL_FINAL(12), //
			;
			
			private final int value;
			
			private AuthenticationResponseType(int value) {
				this.value = value;
			}
		}
	}
	
	public static class ParameterStatus extends PostgresMessages {
		
		private int parameterValueIndex = -1;
		
		public ParameterStatus(ByteBuf data) {
			super(data);
		}
		
		public String getParameterName() {
			final ByteBuf content = content();
			
			content.readerIndex(PAYLOAD_START);
			final String name = readCString(content);
			
			parameterValueIndex = content.readerIndex();
			
			return name;
		}
		
		public String getParameterValue() {
			if (parameterValueIndex == -1) {
				getParameterName();
			}
			
			final ByteBuf content = content();
			content.readerIndex(parameterValueIndex);
			return readCString(content);
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