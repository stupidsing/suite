package suite.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.primitive.Bytes;
import suite.util.os.FileUtil;

/**
 * Defines interface for reading/writing byte buffer. The operation within the
 * same serializer should always put in the same number of bytes.
 */
public class SerializeUtil {

	public static Serializer<Boolean> booleanSerializer = boolean_();
	public static Serializer<Integer> intSerializer = int_();

	public interface Serializer<V> {
		public V read(DataInput dataInput) throws IOException;

		public void write(DataOutput dataOutput, V value) throws IOException;
	}

	/**
	 * Serializes a list.
	 *
	 * Size = 4 + number of elements * size of serializer<T>
	 */
	public static <T> Serializer<List<T>> list(Serializer<T> serializer) {
		return new Serializer<List<T>>() {
			public List<T> read(DataInput dataInput) throws IOException {
				int size = SerializeUtil.intSerializer.read(dataInput);
				List<T> list = new ArrayList<>();
				for (int i = 0; i < size; i++)
					list.add(serializer.read(dataInput));
				return list;
			}

			public void write(DataOutput dataOutput, List<T> list) throws IOException {
				SerializeUtil.intSerializer.write(dataOutput, list.size());
				for (T t : list)
					serializer.write(dataOutput, t);
			}
		};
	}

	/**
	 * Serializes a nullable value.
	 *
	 * Size = 1 + size of serializer<T>
	 */
	public static <T> Serializer<T> nullable(Serializer<T> serializer) {
		return new Serializer<T>() {
			public T read(DataInput dataInput) throws IOException {
				return booleanSerializer.read(dataInput) ? serializer.read(dataInput) : null;
			}

			@Override
			public void write(DataOutput dataOutput, T value) throws IOException {
				boolean isNotNull = value != null;
				booleanSerializer.write(dataOutput, isNotNull);
				if (isNotNull)
					serializer.write(dataOutput, value);
			}
		};
	}

	/**
	 * Serialize bytes.
	 *
	 * Size = length
	 */
	public static Serializer<Bytes> bytes(int length) {
		return new Serializer<Bytes>() {
			public Bytes read(DataInput dataInput) throws IOException {
				byte bs[] = new byte[length];
				dataInput.readFully(bs);
				return Bytes.of(bs);
			}

			public void write(DataOutput dataOutput, Bytes bytes) throws IOException {
				bytes.write(dataOutput);
				for (int i = bytes.size(); i < length; i++)
					dataOutput.write(0);
			}
		};
	}

	/**
	 * Serializes a string as default character set (UTF-8).
	 *
	 * Size = length
	 */
	public static Serializer<String> string(int length) {
		return new Serializer<String>() {
			public String read(DataInput dataInput) throws IOException {
				byte bs[] = new byte[length];
				int l = dataInput.readInt();
				dataInput.readFully(bs);
				return new String(bs, FileUtil.charset).substring(0, l);
			}

			public void write(DataOutput dataOutput, String value) throws IOException {
				byte bs[] = Arrays.copyOf(value.getBytes(FileUtil.charset), length);
				dataOutput.writeInt(value.length());
				dataOutput.write(bs);
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
			public Boolean read(DataInput dataInput) throws IOException {
				return dataInput.readByte() == -1;
			}

			public void write(DataOutput dataOutput, Boolean value) throws IOException {
				dataOutput.writeByte(value ? -1 : 0);
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
			public Integer read(DataInput dataInput) throws IOException {
				return dataInput.readInt();
			}

			public void write(DataOutput dataOutput, Integer value) throws IOException {
				dataOutput.writeInt(value);
			}
		};
	}

}
