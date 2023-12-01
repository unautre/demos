package dev.bandarlog.test.netty.proxy.postgres;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import dev.bandarlog.test.netty.proxy.postgres.PostgresMessages.RequestMessages.SSLNegociationMessage;
import dev.bandarlog.test.netty.proxy.postgres.PostgresMessages.RequestMessages.StartupMessage;
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
			
			final StartupMessage msg;
			if (protocolVersion == SSLNegociationMessage.SSL_NEGOCIATION_PROTOCOL_VERSION) {
				msg = new SSLNegociationMessage();
			} else {
				msg = new StartupMessage();
			}
			
			msg.version = protocolVersion;
			msg.payload = new LinkedHashMap<>();

			// read headers
			// null terminator is expected at the end
			while (in.readableBytes() > 1) {
				final String key = readCString(in);
				System.out.println("Read: " + key);
				
				final String value = readCString(in);
				System.out.println("Read: " + value);
				
				msg.payload.put(key, value);
			}
			
			out.add(msg);
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