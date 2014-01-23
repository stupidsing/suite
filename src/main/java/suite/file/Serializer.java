package suite.file;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.util.FileUtil;

/**
 * Defines interface for reading/writing byte buffer. The operation within the
 * same serializer should always put in the same number of bytes.
 */
public interface Serializer<V> {

	public V read(ByteBuffer buffer);

	public void write(ByteBuffer buffer, V value);

	public static BooleanSerializer booleanSerializer = new BooleanSerializer();
	public static IntSerializer intSerializer = new IntSerializer();

	/**
	 * Serializes a list.
	 * 
	 * Size = 4 + number of elements * size of serializer<T>
	 */
	public static class ListSerializer<T> implements Serializer<List<T>> {
		private Serializer<T> serializer;

		public ListSerializer(Serializer<T> serializer) {
			this.serializer = serializer;
		}

		public List<T> read(ByteBuffer buffer) {
			int size = Serializer.intSerializer.read(buffer);
			List<T> list = new ArrayList<>();
			for (int i = 0; i < size; i++)
				list.add(serializer.read(buffer));
			return list;
		}

		public void write(ByteBuffer buffer, List<T> list) {
			Serializer.intSerializer.write(buffer, list.size());
			for (T t : list)
				serializer.write(buffer, t);
		}
	}

	/**
	 * Serializes a nullable value.
	 * 
	 * Size = 1 + size of serializer<T>
	 */
	public static class NullableSerializer<T> implements Serializer<T> {
		private Serializer<T> serializer;

		public NullableSerializer(Serializer<T> serializer) {
			this.serializer = serializer;
		}

		public T read(ByteBuffer buffer) {
			return booleanSerializer.read(buffer) ? serializer.read(buffer) : null;
		}

		@Override
		public void write(ByteBuffer buffer, T value) {
			boolean isNotNull = value != null;
			booleanSerializer.write(buffer, isNotNull);
			if (isNotNull)
				serializer.write(buffer, value);
		}
	}

	/**
	 * Serializes a byte array.
	 * 
	 * Size = length
	 */
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

	/**
	 * Serializes a string as default character set (UTF-8).
	 * 
	 * Size = length
	 */
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

	/**
	 * Serializes a boolean.
	 * 
	 * Size = 1
	 */
	public static class BooleanSerializer implements Serializer<Boolean> {
		public Boolean read(ByteBuffer buffer) {
			return buffer.get() == -1;
		}

		public void write(ByteBuffer buffer, Boolean value) {
			buffer.put((byte) (value ? -1 : 0));
		}
	}

	/**
	 * Serializes an integer.
	 * 
	 * Size = 4
	 */
	public static class IntSerializer implements Serializer<Integer> {
		public Integer read(ByteBuffer buffer) {
			return buffer.getInt();
		}

		public void write(ByteBuffer buffer, Integer value) {
			buffer.putInt(value);
		}
	}

}
