package suite.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Defines interface for reading/writing byte buffer. The operation within the
 * same serializer should always put in the same number of bytes.
 */
public class SerializeUtil {

	public static Serializer<Boolean> booleanSerializer = boolean_();
	public static Serializer<Integer> intSerializer = int_();

	public interface Serializer<V> {
		public V read(ByteBuffer buffer);

		public void write(ByteBuffer buffer, V value);
	}

	/**
	 * Serializes a list.
	 * 
	 * Size = 4 + number of elements * size of serializer<T>
	 */
	public static <T> Serializer<List<T>> list(final Serializer<T> serializer) {
		return new Serializer<List<T>>() {
			public List<T> read(ByteBuffer buffer) {
				int size = SerializeUtil.intSerializer.read(buffer);
				List<T> list = new ArrayList<>();
				for (int i = 0; i < size; i++)
					list.add(serializer.read(buffer));
				return list;
			}

			public void write(ByteBuffer buffer, List<T> list) {
				SerializeUtil.intSerializer.write(buffer, list.size());
				for (T t : list)
					serializer.write(buffer, t);
			}
		};
	}

	/**
	 * Serializes a nullable value.
	 * 
	 * Size = 1 + size of serializer<T>
	 */
	public static <T> Serializer<T> nullable(final Serializer<T> serializer) {
		return new Serializer<T>() {
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
		};
	}

	/**
	 * Serializes a byte array.
	 * 
	 * Size = length
	 */
	public static Serializer<byte[]> byteArray(final int length) {
		return new Serializer<byte[]>() {
			public byte[] read(ByteBuffer buffer) {
				byte bs[] = new byte[length];
				buffer.get(bs);
				return bs;
			}

			public void write(ByteBuffer buffer, byte value[]) {
				buffer.put(value);
			}
		};
	}

	/**
	 * Serializes a string as default character set (UTF-8).
	 * 
	 * Size = length
	 */
	public static Serializer<String> string(final int length) {
		return new Serializer<String>() {
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
		};
	}

	/**
	 * Serializes a boolean.
	 * 
	 * Size = 1
	 */
	public static Serializer<Boolean> boolean_() {
		return new Serializer<Boolean>() {
			public Boolean read(ByteBuffer buffer) {
				return buffer.get() == -1;
			}

			public void write(ByteBuffer buffer, Boolean value) {
				buffer.put((byte) (value ? -1 : 0));
			}
		};
	}

	/**
	 * Serializes an integer.
	 * 
	 * Size = 4
	 */
	public static Serializer<Integer> int_() {
		return new Serializer<Integer>() {
			public Integer read(ByteBuffer buffer) {
				return buffer.getInt();
			}

			public void write(ByteBuffer buffer, Integer value) {
				buffer.putInt(value);
			}
		};
	}

}
