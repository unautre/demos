package dev.bandarlog.test.netty.proxy.cql;

import static dev.bandarlog.test.netty.proxy.cql.CqlUtils.readStringMap;
import static dev.bandarlog.test.netty.proxy.cql.CqlUtils.readStringMultiMap;

import java.util.List;

import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Options;
import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Startup;
import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Supported;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

public class CqlMessageDecoder extends ByteToMessageDecoder {

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
		// all other messages begin with a letter, and a size (32bit).
		if (in.readableBytes() < 9)
			return;

		in.markReaderIndex();

		final byte version = in.readByte();
		final byte flags = in.readByte();
		final short stream = in.readShort();
		final byte opcode = in.readByte();
		final int length = in.readInt();

		if (in.readableBytes() < length) {
			in.resetReaderIndex();
			return;
		}
		
		System.out.printf("version=%d flags=%d stream=%d opcode=%d length=%d%n", version & 0x1F, flags, stream, opcode, length);		

		// create subframe dedicated for this message
		in = in.readSlice(length);

		switch (opcode & 0x1F) {
		case 1:
			final Startup startup = new Startup();
			startup.setHeader(version >> 7 != 0, (byte) (version & 0x7F), flags, stream);
			startup.payload = readStringMap(in);
			out.add(startup);
			break;
		case 5:
			final Options options = new Options();
			options.setHeader(version >> 7 != 0, (byte) (version & 0x7F), flags, stream);
			out.add(options);
			break;
		case 6:
			final Supported supported = new Supported();
			supported.setHeader(version >> 7 != 0, (byte) (version & 0x7F), flags, stream);
			supported.payload = readStringMultiMap(in);
			out.add(supported);
			break;
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.println("Caught: ");
		cause.printStackTrace();
	}
}