package dev.bandarlog.test.netty.proxy.cql;

import static dev.bandarlog.test.netty.proxy.cql.CqlUtils.*;

import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Options;
import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Ready;
import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Register;
import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Startup;
import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Supported;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class CqlMessageEncoder extends MessageToByteEncoder<CassandraMessages> {

	@Override
	protected void encode(ChannelHandlerContext ctx, CassandraMessages msg, ByteBuf out) throws Exception {
		out.writeByte((byte) (msg.version | (msg.response ? 128 : 0)));
		out.writeByte(msg.flags);
		out.writeShort(msg.stream);
		out.writeByte(msg.opcode);
		
		// length is just after opcode
		final int bodyLengthIndex = out.writerIndex();
		out.writeInt(0);
		
		if (msg instanceof Startup) {
			final Startup startup = (Startup) msg;
			
			writeStringMap(out, startup.payload);
		} else if (msg instanceof Ready) {
			// NOP
		} else if (msg instanceof Options) {
			// NOP
		} else if (msg instanceof Supported) {
			final Supported supported = (Supported) msg;
			
			writeStringMultiMap(out, supported.payload);
		} else if (msg instanceof Register) {
			final Register register = (Register) msg;
			
			writeStringList(out, register.eventTypes);
		} else {
			System.out.println("Unrecognized message " + msg);
		}
		
		final int bodyLength = out.writerIndex() - bodyLengthIndex - 4;
		out.setInt(bodyLengthIndex, bodyLength);
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.println("Caught: ");
		cause.printStackTrace();
	}
}