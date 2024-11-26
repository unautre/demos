package dev.bandarlog.test.netty.protohackers;

import org.junit.Test;

import io.netty.channel.embedded.EmbeddedChannel;

public class InsecureSocketLayerTest {

	@Test
	public void testAppLayer() throws Exception {

		final EmbeddedChannel channel = new EmbeddedChannel(new InsecureSocketLayer.ApplicationLayer());

		channel.writeInbound(
				"78x pocket-size cuddly rocking horse simulator,53x giant soft rubber FPV tractor simulator,75x small rocking horse with inflatable tractor on a string,25x small pony with FPV car on a string,97x small cow with motorcycle simulator");

		System.out.println("Inbound: " + channel.readInbound());
		System.out.println("Outbound: " + channel.readOutbound());
	}
}