package dev.bandarlog.test.netty.proxy.postgres;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class PostgresMessages {

	public static class RequestMessages extends PostgresMessages {

		public static class StartupMessage extends RequestMessages {

			public int version;

			public Map<String, String> payload;
		}

		public static class SSLNegociationMessage extends StartupMessage {

			public static final int SSL_NEGOCIATION_PROTOCOL_VERSION = 80877103;
		}

		public static class PasswordMessage extends RequestMessages {

			public String password;
		}

		public static class Query extends RequestMessages {

			public String query;
		}
		
		public static class Sync extends RequestMessages {
			
		}
		
		public static class Terminate extends RequestMessages {
			
		}
	}

	public static class ResponseMessages extends PostgresMessages {

		public static class NoSSLResponseMessage extends ResponseMessages {

		}

		public static class SSLResponseMessage extends ResponseMessages {

		}

		public static class AuthenticationOK extends ResponseMessages {

		}

		public static class AuthenticationCleartextPassword extends ResponseMessages {

		}

		public static class BackendKeyData extends ResponseMessages {

			public int processId;

			public int secretKey;
		}

		public static class ReadyForQuery extends ResponseMessages {

			public enum TransactionStatusIndicatorEnum {
				NO_TRANSACTION, IN_TRANSACTION, FAILED_TRANSACTION
			}

			public TransactionStatusIndicatorEnum transactionStatusIndicator;
		}

		public static class RowDescription extends ResponseMessages {

			public final List<InnerRowDescription> rows = new ArrayList<>();

			public static class InnerRowDescription {
				public String name;

				public int objectId;

				public short attributeNumber;

				public int dataType;

				public short dataSize;

				public int typeModifier;

				public short formatCode;
			}
		}

		public static class DataRow extends ResponseMessages {

			public final List<String> columns = new ArrayList<>();
		}

		public static class CommandComplete extends ResponseMessages {

			public String commandTag;
		}
	}
}