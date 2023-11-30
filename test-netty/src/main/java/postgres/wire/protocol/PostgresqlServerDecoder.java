package postgres.wire.protocol;

import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class PostgresqlServerDecoder extends ByteToMessageDecoder {

	private enum State {
		EXPECTING_STARTUP_PACKET, EXPECTING_NORMAL_PACKET
	}
	
	private State state;
	
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		if (state == State.EXPECTING_STARTUP_PACKET) {
			if (in.readableBytes() <= 4)
				return;
				
			final int len = in.getInt(in.readerIndex());
				
			if (in.readableBytes() <= len)
				return;
		}
	}
}