package dev.bandarlog.test.netty.protohackers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import dev.bandarlog.test.netty.protohackers.MobInTheMiddle.MITM;
import io.netty.channel.ChannelHandlerContext;

public class MobInTheMiddleTest {

	@Test
	public void test1() throws Exception {
		assertTrue(MITM.PATTERN.matcher("7adNeSwJkMakpEcln9HEtthSRtxdmEHOT8T").find());
		assertTrue(MITM.PATTERN.matcher(" 7adNeSwJkMakpEcln9HEtthSRtxdmEHOT8T ").find());
	}

	@Test
	public void test2() throws Exception {
		final ChannelHandlerContext mock = Mockito.mock(ChannelHandlerContext.class);
		
		final MITM mitm = new MITM();
		
		mitm.channelRead(mock, " 7adNeSwJkMakpEcln9HEtthSRtxdmEHOT8T ");
		
		final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
		
		Mockito.verify(mock).fireChannelRead(captor.capture());
		
		System.out.println(captor.getValue());
		
		assertEquals(" 7YWHMfk9JZe0LM0g1ZauHuiSxhI ", captor.getValue());
	}
}