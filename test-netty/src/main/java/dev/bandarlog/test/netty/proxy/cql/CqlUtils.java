package dev.bandarlog.test.netty.proxy.cql;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.netty.buffer.ByteBuf;

public class CqlUtils {

	private static final Charset CHARSET = StandardCharsets.UTF_8;
	
	static Map<String, String> readStringMap(ByteBuf in) {
		final int total = in.readShort() & 0xFFFF; // unsigned

		final Map<String, String> map = new HashMap<>();
		for (int i = 0; i < total; i++) {
			final String key = readString(in);
			final String value = readString(in);

			map.put(key, value);
		}

		return map;
	}

	static Map<String, List<String>> readStringMultiMap(ByteBuf in) {
		final int total = in.readShort() & 0xFFFF; // unsigned

		final Map<String, List<String>> map = new HashMap<>();
		for (int i = 0; i < total; i++) {
			final String key = readString(in);
			final List<String> value = readStringList(in);

			map.put(key, value);
		}

		return map;
	}

	static List<String> readStringList(ByteBuf in) {
		final int total = in.readShort() & 0xFFFF; // unsigned

		final List<String> list = new ArrayList<>();
		for (int i = 0; i < total; i++) {
			list.add(readString(in));
		}

		return list;
	}

	static String readString(ByteBuf in) {
		final int total = in.readShort() & 0xFFFF; // unsigned

		return in.readCharSequence(total, CHARSET).toString();
	}
	
	static void writeStringMultiMap(ByteBuf in, Map<String, List<String>> map) {
		in.writeShort((short) (map.size() & 0xFFFF));
		
		for (Entry<String, List<String>> entry : map.entrySet()) {
			writeString(in, entry.getKey());
			writeStringList(in, entry.getValue());
		}
	}
	
	static void writeStringMap(ByteBuf in, Map<String, String> map) {
		in.writeShort((short) (map.size() & 0xFFFF));
		
		for (Entry<String, String> entry : map.entrySet()) {
			writeString(in, entry.getKey());
			writeString(in, entry.getValue());
		}
	}
	
	static void writeStringList(ByteBuf in, List<String> l) {
		in.writeShort((short) (l.size() & 0xFFFF));
		
		for (String value : l) {
			writeString(in, value);
		}
	}
	
	static void writeString(ByteBuf in, String s) {
		final int sizeIndex = in.writerIndex();
		in.writeShort(0);
		
		final int size = in.writeCharSequence(s, CHARSET);
		in.setShort(sizeIndex, size);
	}
}