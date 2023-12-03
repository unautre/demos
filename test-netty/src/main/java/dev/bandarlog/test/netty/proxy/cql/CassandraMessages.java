package dev.bandarlog.test.netty.proxy.cql;

import java.util.List;
import java.util.Map;

public class CassandraMessages {

	boolean response;
	
	byte version = 3;
	
	byte flags;
	
	short stream;
	
	void setHeader(boolean response, byte version, byte flags, short stream) {
		this.response = response;
		this.version = version;
		this.flags = flags;
		this.stream = stream;
	}
	
	public static class Startup extends CassandraMessages {
		public Map<String, String> payload;
	}
	
	public static class Options extends CassandraMessages {
		
	}
	
	public static class Supported extends CassandraMessages {
		public Map<String, List<String>> payload;
	}
}