package dev.bandarlog.test.netty.proxy.cql;

import java.util.List;
import java.util.Map;

import dev.bandarlog.test.netty.proxy.cql.CqlUtils.Consistency;

public class CassandraMessages {

	boolean response;
	
	byte version = 3;
	
	byte flags;
	
	short stream;
	
	final byte opcode;
	
	public CassandraMessages(byte opcode) {
		this.opcode = opcode;
	}

	void setHeader(byte version, byte flags, short stream) {
		this.response = version >> 7 != 0;
		this.version = (byte) (version & 0x7F);
		this.flags = flags;
		this.stream = stream;
	}
	
	public static class Error extends CassandraMessages {
		public static final byte OPCODE = 0x00;
		
		public Error() {
			super(OPCODE);
		}
		
		public int code;
		
		public String message;
	}
	
	public static class Startup extends CassandraMessages {
		
		public static final byte OPCODE = 0x01;
		
		public Startup() {
			super(OPCODE);
		}
		
		public Map<String, String> payload;
	}
	
	public static class Ready extends CassandraMessages {
		
		public static final byte OPCODE = 0x02;
		
		public Ready() {
			super(OPCODE);
		}
	}
	
	public static class Authenticate extends CassandraMessages {
		
		public static final byte OPCODE = 0x03;
		
		public Authenticate() {
			super(OPCODE);
		}
		
		public String payload;
	}
	
	public static class Options extends CassandraMessages {
		
		public static final byte OPCODE = 0x05;
		
		public Options() {
			super(OPCODE);
		}
	}
	
	public static class Supported extends CassandraMessages {
		
		public static final byte OPCODE = 0x06;
		
		public Supported() {
			super(OPCODE);
		}
		
		public Map<String, List<String>> payload;
	}
	
	public static class Query extends CassandraMessages {
		public static final byte OPCODE = 0x07;
		
		public Query() {
			super(OPCODE);
		}
		
		public String query;
		
		public Consistency consistency;
		
		public byte queryFlags;

		public List<String> values;
		
		public Map<String, String> namedValues;
		
		public Integer resultPageSize;
		
		public Byte pagingState;
		
		public Consistency serialConsistency;
		
		public Long timestamp;
		
		public static byte VALUES = 0x1;
		public static byte SKIP_METADATA = 0x2;
		public static byte PAGE_SIZE = 0x4;
		public static byte WITH_PAGING_STATE = 0x8;
		public static byte WITH_SERIAL_CONSISTENCY = 0x10;
		public static byte WITH_DEFAULT_TIMESTAMP = 0x20;
		public static byte WITH_NAMES_FOR_VALUES = 0x40;
	}
	
	public static class Result extends CassandraMessages {
		
		public static final byte OPCODE = 0x08;
		
		public Result() {
			super(OPCODE);
		}
		
		public int kind;
		
		public static int VOID = 1;
		public static int ROWS = 2;
		public static int SET_KEYSPACE = 3;
		public static int PREPARED = 4;
		public static int SCHEMA_CHANGE = 5;
	}
	
	public static class Register extends CassandraMessages {
		
		public static final byte OPCODE = 0x0B;
		
		public Register() {
			super(OPCODE);
		}
		
		public List<String> eventTypes;
	}
}