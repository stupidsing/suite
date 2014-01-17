package suite.btree;

import java.nio.ByteBuffer;
import java.util.Arrays;

import suite.util.FileUtil;

/**
 * Defines interface for reading/writing byte buffer. The operation within the
 * same serializer should always put in the same number of bytes.
 */
public interface Serializer<V> {

	public V read(ByteBuffer buffer);

	public void write(ByteBuffer buffer, V value);

	public static class ByteArraySerializer implements Serializer<byte[]> {
		private int length;

		public ByteArraySerializer(int length) {
			this.length = length;
		}

		public byte[] read(ByteBuffer buffer) {
			byte bs[] = new byte[length];
			buffer.get(bs);
			return bs;
		}

		public void write(ByteBuffer buffer, byte value[]) {
			buffer.put(value);
		}
	}

	public static class StringSerializer implements Serializer<String> {
		private int length;

		public StringSerializer(int length) {
			this.length = length;
		}

		public String read(ByteBuffer buffer) {
			byte bs[] = new byte[length];
			int l = buffer.getInt();
			buffer.get(bs);
			return new String(bs, FileUtil.charset).substring(0, l);
		}

		public void write(ByteBuffer buffer, String value) {
			byte bs[] = Arrays.copyOf(value.getBytes(FileUtil.charset), length);
			buffer.putInt(value.length());
			buffer.put(bs);
		}
	}

	public static class IntSerializer implements Serializer<Integer> {
		public Integer read(ByteBuffer buffer) {
			return buffer.getInt();
		}

		public void write(ByteBuffer buffer, Integer value) {
			buffer.putInt(value);
		}
	}

}
