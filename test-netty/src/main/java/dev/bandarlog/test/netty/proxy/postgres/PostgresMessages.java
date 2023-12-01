package dev.bandarlog.test.netty.proxy.postgres;

import java.util.Map;

public class PostgresMessages {

	public static class RequestMessages extends PostgresMessages {

		public static class StartupMessage extends RequestMessages {
		
			public int version;

			public Map<String, String> payload;

			@Override
			public String toString() {
				return "StartupMessage [version=" + version + ", payload=" + payload + "]";
			}
		}
		
		public static class SSLNegociationMessage extends StartupMessage {
			
			public static final int SSL_NEGOCIATION_PROTOCOL_VERSION = 80877103;
		}
	}
	
	public static class ResponseMessages extends PostgresMessages {
		
		public static class NoSSLResponseMessage extends ResponseMessages {
			
		}
		
		public static class SSLResponseMessage extends ResponseMessages {
			
		}
	}
}