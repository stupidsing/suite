package suite.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import suite.Constants;
import suite.adt.Pair;
import suite.file.ExtentAllocator.Extent;
import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.primitive.Bytes;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.FunUtil.Fun;

/**
 * Defines interface for reading/writing byte buffer. The operation within the
 * same serializer should always put in the same number of bytes.
 */
public class Serialize {

	private static Inspect inspect = Singleton.get().getInspect();
	private static byte[] zeroes = new byte[4096];

	public static Serializer<Boolean> boolean_ = boolean_();
	public static Serializer<Double> double_ = double_();
	public static Serializer<Float> float_ = float_();
	public static Serializer<Integer> int_ = int_();

	public static <T> Serializer<T> auto(Class<T> clazz) {
		Serializer<?> serializer0 = memoizeAutoSerializers.apply(clazz);
		@SuppressWarnings("unchecked")
		Serializer<T> serializer = (Serializer<T>) serializer0;
		return serializer;
	}

	private static Fun<Type, Serializer<?>> memoizeAutoSerializers = Memoize.fun(Serialize::auto_);

	// do not handle nulls
	private static <T> Serializer<?> auto_(Type type) {
		Serializer<?> serializer;
		if (type instanceof Class) {
			Class<?> clazz = (Class<?>) type;
			if (Objects.equals(clazz, boolean.class) || Objects.equals(clazz, Boolean.class))
				serializer = boolean_;
			else if (Objects.equals(clazz, Bytes.class))
				serializer = variableLengthBytes;
			else if (Objects.equals(clazz, double.class) || Objects.equals(clazz, Double.class))
				serializer = double_;
			else if (Objects.equals(clazz, float.class) || Objects.equals(clazz, Float.class))
				serializer = float_;
			else if (Objects.equals(clazz, float[].class))
				serializer = arrayOfFloats;
			else if (Objects.equals(clazz, int.class) || Objects.equals(clazz, Integer.class))
				serializer = int_;
			else if (Objects.equals(clazz, String.class))
				serializer = variableLengthString;
			else if (clazz.isArray()) {
				@SuppressWarnings("unchecked")
				Class<Object> c1 = (Class<Object>) clazz.getComponentType();
				@SuppressWarnings("unchecked")
				Serializer<Object> serializer1 = (Serializer<Object>) auto_(c1);
				serializer = array(c1, serializer1);
			} else if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()))
				serializer = poly(clazz);
			else
				serializer = autoFields(clazz);
		} else if (type instanceof ParameterizedType) {
			ParameterizedType pt = (ParameterizedType) type;
			Type rawType = pt.getRawType();
			Type[] typeArgs = pt.getActualTypeArguments();
			Class<?> clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (List.class.isAssignableFrom(clazz))
				serializer = list(auto_(typeArgs[0]));
			else if (Map.class.isAssignableFrom(clazz))
				serializer = map(auto_(typeArgs[0]), auto_(typeArgs[1]));
			else if (Pair.class.isAssignableFrom(clazz))
				serializer = pair(auto_(typeArgs[0]), auto_(typeArgs[1]));
			else
				throw new RuntimeException();
		} else
			throw new RuntimeException();
		return serializer;
	}

	public static <T> Serializer<T> autoFields(Class<T> clazz) {
		Pair<Field, ?>[] pairs = Read.from(inspect.fields(clazz)) //
				.map2(field -> auto_(field.getGenericType())) //
				.toArray();

		Streamlet<Constructor<?>> ctors = Read.from(clazz.getConstructors());
		boolean isDefaultCtor = 0 < ctors.filter(ctor -> ctor.getParameterCount() == 0).size();
		Constructor<?> immutableCtor = ctors.min((c0, c1) -> -Integer.compare(c0.getParameterCount(), c1.getParameterCount()));

		Serializer<?> serializer0 = new Serializer<T>() {
			public T read(DataInput dataInput) throws IOException {
				try {
					Object object;
					if (isDefaultCtor) {
						object = clazz.newInstance();
						for (Pair<Field, ?> pair : pairs)
							pair.t0.set(object, ((Serializer<?>) pair.t1).read(dataInput));
					} else {
						Object[] ps = new Object[immutableCtor.getParameterCount()];
						for (int i = 0; i < ps.length; i++) {
							Pair<Field, ?> pair = pairs[i];
							ps[i] = ((Serializer<?>) pair.t1).read(dataInput);
						}
						object = immutableCtor.newInstance(ps);
					}
					@SuppressWarnings("unchecked")
					T t = (T) object;
					return t;
				} catch (ReflectiveOperationException ex) {
					throw new RuntimeException(ex);
				}
			}

			public void write(DataOutput dataOutput, T t) throws IOException {
				for (Pair<Field, ?> pair : pairs) {
					@SuppressWarnings("unchecked")
					Serializer<Object> serializer1 = (Serializer<Object>) pair.t1;
					serializer1.write(dataOutput, Rethrow.ex(() -> pair.t0.get(t)));
				}

			}
		};
		@SuppressWarnings("unchecked")
		Serializer<T> serializer = (Serializer<T>) serializer0;
		return serializer;
	}

	public static Serializer<Bytes> variableLengthBytes = new Serializer<Bytes>() {
		public Bytes read(DataInput dataInput) throws IOException {
			int length = dataInput.readInt();
			byte[] bs = new byte[length];
			dataInput.readFully(bs);
			return Bytes.of(bs);
		}

		public void write(DataOutput dataOutput, Bytes bytes) throws IOException {
			dataOutput.writeInt(bytes.size());
			bytes.write(dataOutput);
		}
	};

	public static Serializer<String> variableLengthString = new Serializer<String>() {
		public String read(DataInput dataInput) throws IOException {
			return dataInput.readUTF();
		}

		public void write(DataOutput dataOutput, String value) throws IOException {
			dataOutput.writeUTF(value);
		}
	};

	public interface Serializer<V> {
		public V read(DataInput dataInput) throws IOException;

		public void write(DataOutput dataOutput, V value) throws IOException;
	}

	public static <T> Serializer<T[]> array(Class<T> clazz, Serializer<T> serializer) {
		return new Serializer<T[]>() {
			public T[] read(DataInput dataInput) throws IOException {
				int size = Serialize.int_.read(dataInput);
				T[] array = Util.newArray(clazz, size);
				for (int i = 0; i < size; i++)
					array[i] = serializer.read(dataInput);
				return array;
			}

			public void write(DataOutput dataOutput, T[] array) throws IOException {
				Serialize.int_.write(dataOutput, array.length);
				for (T t : array)
					serializer.write(dataOutput, t);
			}
		};
	}

	public static Serializer<float[]> arrayOfFloats = new Serializer<float[]>() {
		public float[] read(DataInput dataInput) throws IOException {
			int size = Serialize.int_.read(dataInput);
			float[] array = new float[size];
			for (int i = 0; i < size; i++)
				array[i] = dataInput.readFloat();
			return array;
		}

		public void write(DataOutput dataOutput, float[] array) throws IOException {
			Serialize.int_.write(dataOutput, array.length);
			for (float t : array)
				dataOutput.writeFloat(t);
		}
	};

	/**
	 * Serialize bytes.
	 *
	 * Size = length
	 */
	public static Serializer<Bytes> bytes(int length) {
		return new Serializer<Bytes>() {
			public Bytes read(DataInput dataInput) throws IOException {
				byte[] bs = new byte[length];
				dataInput.readFully(bs);
				return Bytes.of(bs);
			}

			public void write(DataOutput dataOutput, Bytes bytes) throws IOException {
				bytes.write(dataOutput);
				int i = bytes.size();
				while (i < length) {
					int i1 = Math.min(i + zeroes.length, length);
					dataOutput.write(zeroes, 0, i1 - i);
					i = i1;
				}
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

	public static <K, V> Serializer<Map<K, V>> map(Serializer<K> ks, Serializer<V> vs) {
		return map_(ks, vs);
	}

	public static <V> Serializer<Map<String, V>> mapOfString(Serializer<V> vs) {
		return map_(variableLengthString, vs);
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

	/**
	 * Serializes a string as default character set (UTF-8).
	 *
	 * Size = length
	 */
	public static Serializer<String> string(int length) {
		return new Serializer<String>() {
			public String read(DataInput dataInput) throws IOException {
				byte[] bs = new byte[length];
				int l = dataInput.readInt();
				dataInput.readFully(bs);
				return To.string(bs).substring(0, l);
			}

			public void write(DataOutput dataOutput, String value) throws IOException {
				byte[] bs = Arrays.copyOf(value.getBytes(Constants.charset), length);
				dataOutput.writeInt(value.length());
				dataOutput.write(bs);
			}
		};
	}

	public static <T> Serializer<T> verify(Object o, Serializer<T> serializer) {
		int c = o.hashCode();

		return new Serializer<T>() {
			public T read(DataInput dataInput) throws IOException {
				if (dataInput.readInt() == c)
					return serializer.read(dataInput);
				else
					throw new RuntimeException();
			}

			public void write(DataOutput dataOutput, T value) throws IOException {
				dataOutput.writeInt(c);
				serializer.write(dataOutput, value);
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
	 * Serializes an double.
	 *
	 * Size = 8
	 */
	private static Serializer<Double> double_() {
		return new Serializer<Double>() {
			public Double read(DataInput dataInput) throws IOException {
				return dataInput.readDouble();
			}

			public void write(DataOutput dataOutput, Double value) throws IOException {
				dataOutput.writeDouble(value);
			}
		};
	}

	/**
	 * Serializes an float.
	 *
	 * Size = 4
	 */
	private static Serializer<Float> float_() {
		return new Serializer<Float>() {
			public Float read(DataInput dataInput) throws IOException {
				return dataInput.readFloat();
			}

			public void write(DataOutput dataOutput, Float value) throws IOException {
				dataOutput.writeFloat(value);
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

	private static <K, V> Serializer<Map<K, V>> map_(Serializer<K> ks, Serializer<V> vs) {
		return new Serializer<Map<K, V>>() {
			public Map<K, V> read(DataInput dataInput) throws IOException {
				int size = Serialize.int_.read(dataInput);
				Map<K, V> map = new HashMap<>();
				for (int i = 0; i < size; i++) {
					K k = ks.read(dataInput);
					V v = vs.read(dataInput);
					map.put(k, v);
				}
				return map;
			}

			public void write(DataOutput dataOutput, Map<K, V> map) throws IOException {
				Serialize.int_.write(dataOutput, map.size());
				for (Entry<K, V> entry : map.entrySet()) {
					ks.write(dataOutput, entry.getKey());
					vs.write(dataOutput, entry.getValue());
				}
			}
		};
	}

	private static <T> Serializer<T> poly(Class<T> interface_) {
		return new Serializer<T>() {
			public T read(DataInput dataInput) throws IOException {
				Class<?> c = Rethrow.ex(() -> Class.forName(dataInput.readUTF()));
				if (interface_.isAssignableFrom(c)) {
					@SuppressWarnings("unchecked")
					T t = (T) auto(c).read(dataInput);
					return t;
				} else
					throw new RuntimeException(c.getSimpleName() + " does not implement " + interface_.getSimpleName());
			}

			public void write(DataOutput dataOutput, T t) throws IOException {
				@SuppressWarnings("unchecked")
				Class<Object> c = (Class<Object>) t.getClass();
				if (interface_.isAssignableFrom(c)) {
					dataOutput.writeUTF(c.getName());
					auto(c).write(dataOutput, t);
				} else
					throw new RuntimeException(c.getSimpleName() + " does not implement " + interface_.getSimpleName());

			}
		};
	}

}
