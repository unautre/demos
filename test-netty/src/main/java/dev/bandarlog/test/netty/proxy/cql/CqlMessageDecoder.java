package dev.bandarlog.test.netty.proxy.cql;

import static dev.bandarlog.test.netty.proxy.cql.CqlUtils.readLongString;
import static dev.bandarlog.test.netty.proxy.cql.CqlUtils.readString;
import static dev.bandarlog.test.netty.proxy.cql.CqlUtils.readStringList;
import static dev.bandarlog.test.netty.proxy.cql.CqlUtils.readStringMap;
import static dev.bandarlog.test.netty.proxy.cql.CqlUtils.readStringMultiMap;

import java.util.LinkedHashMap;
import java.util.List;

import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Error;
import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Options;
import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Query;
import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Ready;
import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Register;
import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Result;
import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Startup;
import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Supported;
import dev.bandarlog.test.netty.proxy.cql.CqlUtils.Consistency;
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

		CassandraMessages m = null;
		switch (opcode & 0x1F) {
		case Error.OPCODE: // 0x0
			final Error error = new Error();
			error.code = in.readInt();
			error.message = readString(in);
			m = error;
			break;
		case Startup.OPCODE: // 0x1
			final Startup startup = new Startup();
			startup.setHeader(version, flags, stream);
			startup.payload = readStringMap(in);
			m = startup;
			break;
		case Ready.OPCODE: // 0x2
			final Ready ready = new Ready();
			m = ready;
			break;
		case Options.OPCODE: // 0x5
			final Options options = new Options();
			m = options;
			break;
		case Supported.OPCODE: // 0x6
			final Supported supported = new Supported();
			supported.payload = readStringMultiMap(in);
			m = supported;
			break;
		case Query.OPCODE: // 0x7
			final Query query = new Query();
			
			query.query = readLongString(in);
			query.consistency = Consistency.valueOf(in.readShort());
			query.flags = in.readByte();
			
			if ((query.flags & Query.VALUES) != 0) {
				final short numberOfValues = in.readShort();
				
				
				if ((query.flags & Query.WITH_NAMES_FOR_VALUES) != 0) {
					query.namedValues = new LinkedHashMap<>();
					
					for (int i = 0; i < numberOfValues; i++) {
//						final 
					}
				}
				throw new UnsupportedOperationException();
			}
			break;
		case Result.OPCODE:
			final Result result = new Result();
//			result.
		case Register.OPCODE: // 0xB
			final Register register = new Register();
			register.eventTypes = readStringList(in);
			m = register;
			break;
		}
		
		if (m != null) {
			m.setHeader(version, flags, stream);
			out.add(m);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.println("Caught: ");
		cause.printStackTrace();
	}
}