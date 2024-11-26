package com.bolyartech.scram_sasl.client;

import static org.junit.Assert.assertEquals;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.junit.Test;

import com.bolyartech.scram_sasl.client.ScramSaslClientProcessor.Listener;
import com.bolyartech.scram_sasl.client.ScramSaslClientProcessor.Sender;

public class ScramSaslClientProcessorTest {

	@Test
	public void test1() throws Exception {
		final Listener listener = new Listener() {
			
			@Override
			public void onSuccess() {
				System.out.println("On success");
			}
			
			@Override
			public void onFailure() {
				System.out.println("On failure");
			}
		};
		
		final Sender sender = new Sender() {
			
			private final Queue<String> expected = new ArrayDeque<>();
			
			{
				expected.add("n,,n=,r=2T8gIdnngwE6Nm9vDFJAzpeI");
				expected.add("c=biws,r=2T8gIdnngwE6Nm9vDFJAzpeIJQFAc43oEssO+vvWbMPikipS,p=4/pCUSDLziQsA3hORLb9md0ViUGkFi+Xdl/pC8aYRhs=");
			}
			
			@Override
			public void sendMessage(String msg) {
				System.out.println("Sending message: " + msg);
				assertEquals(expected.remove(), msg);
			}
		};

		final AbstractScramSaslClientProcessor processor = new AbstractScramSaslClientProcessor(listener, sender, "SHA-256", "HmacSHA256", "2T8gIdnngwE6Nm9vDFJAzpeI") {};
		
		processor.start("", "sasha");
		
		processor.onMessage("r=2T8gIdnngwE6Nm9vDFJAzpeIJQFAc43oEssO+vvWbMPikipS,s=ridpK/25HDt9W5uXl57NvA==,i=4096");
		processor.onMessage("v=ad7PY9iujtLyUB5+8k8bk1shYWQ8FZi+oHh5d3xQ0Mk=");
	}
}