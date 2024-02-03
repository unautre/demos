package dev.bandarlog.test.netty.proxy.postgres.lean.codec;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import dev.bandarlog.test.netty.proxy.postgres.lean.codec.PostgresMessages.AuthenticationResponse;
import dev.bandarlog.test.netty.proxy.postgres.lean.codec.PostgresMessages.Password;
import dev.bandarlog.test.netty.proxy.postgres.lean.codec.PostgresMessages.AuthenticationResponse.AuthenticationResponseType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class MessageFactory {

	private static final Charset CHARSET = StandardCharsets.US_ASCII;

	public static AuthenticationResponse buildAuthenticationResponse(ByteBufAllocator alloc, AuthenticationResponseType type) {
		final ByteBuf buf = alloc.buffer(9);
		
		buf.writeByte('R');
		buf.writeInt(8);
		buf.writeInt(type.value);
		
		return new AuthenticationResponse(buf);
	}
	
	public static Password buildPassword(ByteBufAllocator alloc, String password) {
		final int size = 4 + password.length() + 1;
		final ByteBuf buf = alloc.buffer(1 + size);
		
		buf.writeByte('p');
		buf.writeInt(size);
		buf.writeCharSequence(password, CHARSET);
		buf.writeByte(0);
		
		return new Password(buf);
	}
	
	public static Password buildSASLResponse(ByteBufAllocator alloc, String password) {
		final int size = 4 + password.length();
		final ByteBuf buf = alloc.buffer(1 + size);
		
		buf.writeByte('p');
		buf.writeInt(size);
		buf.writeCharSequence(password, CHARSET);
		
		return new Password(buf);
	}
	
	public static PostgresMessages buildSASLInitialResponse(ByteBufAllocator alloc, String challenge, CharSequence initialResponse) {
		final int size = 4 + challenge.length() + 1 + 4 + initialResponse.length();
		final ByteBuf buf = alloc.buffer(1 + size);
		
		buf.writeByte('p');
		buf.writeInt(size);
		buf.writeCharSequence(challenge, CHARSET);
		buf.writeByte(0);
		buf.writeInt(initialResponse.length());
		buf.writeCharSequence(initialResponse, CHARSET);
		
		return new PostgresMessages(buf);
	}
}