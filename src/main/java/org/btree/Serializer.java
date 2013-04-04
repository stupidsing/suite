package org.btree;

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.util.IoUtil;

/**
 * Defines interface for reading/writing byte buffer. The operation within the
 * same accessor should always put in same number of bytes.
 */
public interface Serializer<V> {

	public V read(ByteBuffer buffer);

	public void write(ByteBuffer buffer, V value);

	public static class IntSerializer implements Serializer<Integer> {
		public Integer read(ByteBuffer buffer) {
			return buffer.getInt();
		}

		public void write(ByteBuffer buffer, Integer value) {
			buffer.putInt(value);
		}
	}

	public static class FixedStringSerializer implements Serializer<String> {
		private int length;

		public FixedStringSerializer(int length) {
			this.length = length;
		}

		public String read(ByteBuffer buffer) {
			byte bs[] = new byte[length];
			int l = buffer.getInt();
			buffer.get(bs);
			return new String(bs, IoUtil.charset).substring(0, l);
		}

		public void write(ByteBuffer buffer, String value) {
			byte bs[] = Arrays.copyOf(value.getBytes(IoUtil.charset), length);
			buffer.putInt(value.length());
			buffer.put(bs);
		}
	}

}
