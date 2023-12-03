package dev.bandarlog.test.netty.proxy.cql;

import static dev.bandarlog.test.netty.proxy.cql.CqlUtils.*;

import dev.bandarlog.test.netty.proxy.cql.CassandraMessages.Options;
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
		
		if (msg instanceof Startup) {
			final Startup startup = (Startup) msg;
			
			out.writeByte(1);
			out.writeInt(0);
			
			final int bodyLengthIndex = out.writerIndex();
			out.writeInt(0);
			
			writeStringMap(out, startup.payload);
			
			final int bodyLength = out.writerIndex() - bodyLengthIndex - 4;
			out.setInt(bodyLengthIndex, bodyLength);
		} else if (msg instanceof Options) {
			out.writeByte(5);
			out.writeInt(0);
		} else if (msg instanceof Supported) {
			final Supported supported = (Supported) msg;
			
			out.writeByte(6);
			
			final int bodyLengthIndex = out.writerIndex();
			out.writeInt(0);
			
			writeStringMultiMap(out, supported.payload);
			
			final int bodyLength = out.writerIndex() - bodyLengthIndex - 4;
			out.setInt(bodyLengthIndex, bodyLength);
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		System.out.println("Caught: ");
		cause.printStackTrace();
	}
}