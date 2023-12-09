package dev.bandarlog.test.netty.proxy.postgres.full;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.AuthenticationCleartextPassword;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.AuthenticationOK;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.BackendKeyData;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.CommandComplete;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.DataRow;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.NoSSLResponseMessage;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.ReadyForQuery;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.RowDescription;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.SSLResponseMessage;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.RowDescription.InnerRowDescription;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class PostgresResponseEncoder extends MessageToByteEncoder<ResponseMessages> {

	private static final Charset CHARSET = StandardCharsets.US_ASCII;

	@Override
	protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, ResponseMessages msg, boolean preferDirect)
			throws Exception {
		if (preferDirect) {
			return ctx.alloc().ioBuffer();
		} else {
			return ctx.alloc().heapBuffer();
		}
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, ResponseMessages msg, ByteBuf out) throws Exception {
		if (msg instanceof NoSSLResponseMessage) {
			out.writeByte('N');
		} else if (msg instanceof SSLResponseMessage) {
			out.writeByte('S');
		} else if (msg instanceof AuthenticationOK) {
			out.writeByte('R');
			out.writeInt(8);
			out.writeInt(0);
		} else if (msg instanceof AuthenticationCleartextPassword) {
			out.writeByte('R');
			out.writeInt(8);
			out.writeInt(3);
		} else if (msg instanceof BackendKeyData) {
			final BackendKeyData data = (BackendKeyData) msg;

			out.writeByte('K');
			out.writeInt(12);
			out.writeInt(data.processId);
			out.writeInt(data.secretKey);
		} else if (msg instanceof ReadyForQuery) {
			final ReadyForQuery data = (ReadyForQuery) msg;

			out.writeByte('Z');
			out.writeInt(5);
			out.writeByte(new byte[] { 'I', 'T', 'E' }[data.transactionStatusIndicator.ordinal()]);
		} else if (msg instanceof RowDescription) {
			final RowDescription data = (RowDescription) msg;

			out.writeByte('T');

			// write size after
			final int sizeIndex = out.writerIndex();
			out.writeInt(0);
			out.writeShort(data.rows.size());

			for (InnerRowDescription entry : data.rows) {
				out.writeCharSequence(entry.name, CHARSET);
				out.writeByte(0);

				out.writeInt(entry.objectId);
				out.writeShort(entry.attributeNumber);
				out.writeInt(entry.dataType);
				out.writeShort(entry.dataSize);
				out.writeInt(entry.typeModifier);
				out.writeShort(entry.formatCode);
			}

			final int size = out.writerIndex();
			out.setInt(sizeIndex, size - 1);
		} else if (msg instanceof DataRow) {
			final DataRow data = (DataRow) msg;

			out.writeByte('D');

			final int sizeIndex = out.writerIndex();
			out.writeInt(0);
			out.writeShort(data.columns.size());

			for (String column : data.columns) {
				out.writeInt(column.length());
				out.writeCharSequence(column, CHARSET);
			}

			out.setInt(sizeIndex, out.writerIndex() - 1);
		} else if (msg instanceof CommandComplete) {
			final CommandComplete data = (CommandComplete) msg;

			out.writeByte('C');
			out.writeInt(data.commandTag.length() + 5);
			out.writeCharSequence(data.commandTag, CHARSET);
			out.writeByte(0);
		}
	}
}