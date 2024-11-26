package dev.bandarlog.test.netty.proxy.postgres.full;

import java.util.Map.Entry;

import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.RequestMessages.CancelRequestMessage;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.RequestMessages.PasswordMessage;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.RequestMessages.Query;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.RequestMessages.SSLNegociationMessage;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.RequestMessages.StartupMessage;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.RequestMessages.Terminate;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.AuthenticationCleartextPassword;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.AuthenticationOK;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.BackendKeyData;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.CommandComplete;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.DataRow;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.NoSSLResponseMessage;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.ReadyForQuery;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.RowDescription;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.ReadyForQuery.TransactionStatusIndicatorEnum;
import dev.bandarlog.test.netty.proxy.postgres.full.PostgresMessages.ResponseMessages.RowDescription.InnerRowDescription;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class PostgresServerMock extends SimpleChannelInboundHandler<PostgresMessages> {

	private static final ChannelFutureListener FIRE = ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, PostgresMessages msg) throws Exception {
		if (msg instanceof CancelRequestMessage) {
			final CancelRequestMessage cancel = (CancelRequestMessage) msg;
			
			System.out.println("Receiving cancel request for " + cancel.processId + "/" + cancel.secretKey);
			
			ctx.close().addListener(FIRE);
		} else if (msg instanceof SSLNegociationMessage) {
			ctx.writeAndFlush(new NoSSLResponseMessage()).addListener(FIRE);
		} else if (msg instanceof StartupMessage) {
			final StartupMessage startup = (StartupMessage) msg;

			System.out.println("Receiving connection:");
			for (Entry<String, String> entry : startup.payload.entrySet()) {
				System.out.println("\t" + entry);
			}

			final AuthenticationCleartextPassword authentication = new AuthenticationCleartextPassword();
			ctx.write(authentication).addListener(FIRE);

			ctx.flush();
		} else if (msg instanceof PasswordMessage) {
			final PasswordMessage password = (PasswordMessage) msg;

			System.out.println("Got password: " + password.password);

			final AuthenticationOK authenticationOk = new AuthenticationOK();
			ctx.write(authenticationOk).addListener(FIRE);

			final BackendKeyData backendKeyData = new BackendKeyData();
			backendKeyData.processId = 42;
			backendKeyData.secretKey = 23;
			ctx.write(backendKeyData).addListener(FIRE);

			final ReadyForQuery readyForQuery = new ReadyForQuery();
			readyForQuery.transactionStatusIndicator = TransactionStatusIndicatorEnum.NO_TRANSACTION;
			ctx.write(readyForQuery).addListener(FIRE);

			ctx.flush();
		} else if (msg instanceof Query) {
			final Query query = (Query) msg;

			System.out.println("Received query: " + query.query);

			final InnerRowDescription rowDesc1 = new InnerRowDescription();
			rowDesc1.name = "id";
			rowDesc1.objectId = 0;
			rowDesc1.attributeNumber = 0;
			rowDesc1.dataType = 23;
			rowDesc1.dataSize = 4;
			rowDesc1.typeModifier = -1;
			rowDesc1.formatCode = 0;

			final InnerRowDescription rowDesc2 = new InnerRowDescription();
			rowDesc2.name = "name";
			rowDesc2.objectId = 0;
			rowDesc2.attributeNumber = 0;
			rowDesc2.dataType = 25;
			rowDesc2.dataSize = -1;
			rowDesc2.typeModifier = -1;
			rowDesc2.formatCode = 0;

			final RowDescription rowDesc = new RowDescription();
			rowDesc.rows.add(rowDesc1);
			rowDesc.rows.add(rowDesc2);

			ctx.write(rowDesc).addListener(FIRE);

			final DataRow dataRow1 = new DataRow();
			dataRow1.columns.add("1");
			dataRow1.columns.add("one");

			ctx.write(dataRow1).addListener(FIRE);

			final DataRow dataRow2 = new DataRow();
			dataRow2.columns.add("2");
			dataRow2.columns.add("two");

			ctx.write(dataRow2).addListener(FIRE);

			final CommandComplete cmdComplete = new CommandComplete();
			cmdComplete.commandTag = "SELECT 2";

			ctx.write(cmdComplete).addListener(FIRE);

			final ReadyForQuery readyForQuery = new ReadyForQuery();
			readyForQuery.transactionStatusIndicator = TransactionStatusIndicatorEnum.NO_TRANSACTION;
			ctx.write(readyForQuery).addListener(FIRE);

			ctx.flush();
		} else if (msg instanceof Terminate) {
			System.out.println("Good bye !");

			ctx.close().addListener(FIRE);
		}
	}
}