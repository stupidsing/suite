package suite.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.adt.Pair;
import suite.file.ExtentAllocator.Extent;
import suite.os.FileUtil;
import suite.primitive.Bytes;

/**
 * Defines interface for reading/writing byte buffer. The operation within the
 * same serializer should always put in the same number of bytes.
 */
public class Serialize {

	public static Serializer<Boolean> boolean_ = boolean_();
	public static Serializer<Integer> int_ = int_();

	public interface Serializer<V> {
		public V read(DataInput dataInput) throws IOException;

		public void write(DataOutput dataOutput, V value) throws IOException;
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

	public static Serializer<Extent> extent() {
		return new Serializer<Extent>() {
			public Extent read(DataInput dataInput) throws IOException {
				int start = dataInput.readInt();
				int end = dataInput.readInt();
				return new Extent(start, end);
			}

			public void write(DataOutput dataOutput, Extent value) throws IOException {
				dataOutput.writeInt(value.start);
				dataOutput.writeInt(value.end);
			}
		};
	}

	/**
	 * Serializes a list.
	 *
	 * Size = 4 + number of elements * size of serializer<T>
	 */
	public static <T> Serializer<List<T>> list(Serializer<T> serializer) {
		return new Serializer<List<T>>() {
			public List<T> read(DataInput dataInput) throws IOException {
				int size = Serialize.int_.read(dataInput);
				List<T> list = new ArrayList<>();
				for (int i = 0; i < size; i++)
					list.add(serializer.read(dataInput));
				return list;
			}

			public void write(DataOutput dataOutput, List<T> list) throws IOException {
				Serialize.int_.write(dataOutput, list.size());
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
				return boolean_.read(dataInput) ? serializer.read(dataInput) : null;
			}

			@Override
			public void write(DataOutput dataOutput, T value) throws IOException {
				boolean isNotNull = value != null;
				boolean_.write(dataOutput, isNotNull);
				if (isNotNull)
					serializer.write(dataOutput, value);
			}
		};
	}

	public static <T0, T1> Serializer<Pair<T0, T1>> pair(Serializer<T0> serializer0, Serializer<T1> serializer1) {
		return new Serializer<Pair<T0, T1>>() {
			public Pair<T0, T1> read(DataInput dataInput) throws IOException {
				T0 t0 = serializer0.read(dataInput);
				T1 t1 = serializer1.read(dataInput);
				return Pair.of(t0, t1);
			}

			public void write(DataOutput dataOutput, Pair<T0, T1> pair) throws IOException {
				serializer0.write(dataOutput, pair.t0);
				serializer1.write(dataOutput, pair.t1);
			}
		};
	}

	public static Serializer<Bytes> variableLengthBytes() {
		return new Serializer<Bytes>() {
			public Bytes read(DataInput dataInput) throws IOException {
				int length = dataInput.readInt();
				byte bs[] = new byte[length];
				dataInput.readFully(bs);
				return Bytes.of(bs);
			}

			public void write(DataOutput dataOutput, Bytes bytes) throws IOException {
				dataOutput.writeInt(bytes.size());
				bytes.write(dataOutput);
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
	private static Serializer<Boolean> boolean_() {
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
	private static Serializer<Integer> int_() {
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
