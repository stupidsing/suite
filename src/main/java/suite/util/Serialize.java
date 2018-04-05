package suite.util;

import static suite.util.Friends.min;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import suite.Constants;
import suite.adt.pair.Pair;
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

	public static Serialize me = new Serialize();

	private Inspect inspect = Singleton.me.inspect;
	private byte[] zeroes = new byte[4096];

	public Serializer<Boolean> boolean_ = boolean_();
	public Serializer<Double> double_ = double_();
	public Serializer<Float> float_ = float_();
	public Serializer<Integer> int_ = int_();

	private Serialize() {
	}

	public <T> Serializer<T> auto(Class<T> clazz) {
		var serializer0 = memoizeAutoSerializers.apply(clazz);
		@SuppressWarnings("unchecked")
		var serializer = (Serializer<T>) serializer0;
		return serializer;
	}

	private Fun<Type, Serializer<?>> memoizeAutoSerializers = Memoize.fun(this::auto_);

	// do not handle nulls
	private <T> Serializer<?> auto_(Type type) {
		Serializer<?> serializer;
		if (type instanceof Class) {
			var clazz = (Class<?>) type;
			if (Objects.equals(clazz, boolean.class) || Objects.equals(clazz, Boolean.class))
				serializer = boolean_;
			else if (Objects.equals(clazz, Bytes.class))
				serializer = variableLengthBytes;
			else if (Objects.equals(clazz, double.class) || Objects.equals(clazz, Double.class))
				serializer = double_;
			else if (Objects.equals(clazz, float.class) || Objects.equals(clazz, Float.class))
				serializer = float_;
			else if (Objects.equals(clazz, float[].class))
				serializer = vector;
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
			var pt = (ParameterizedType) type;
			var rawType = pt.getRawType();
			var typeArgs = pt.getActualTypeArguments();
			var clazz = rawType instanceof Class ? (Class<?>) rawType : null;

			if (Collection.class.isAssignableFrom(clazz))
				serializer = collection(auto_(typeArgs[0]));
			else if (Map.class.isAssignableFrom(clazz))
				serializer = map(auto_(typeArgs[0]), auto_(typeArgs[1]));
			else if (Pair.class.isAssignableFrom(clazz))
				serializer = pair(auto_(typeArgs[0]), auto_(typeArgs[1]));
			else
				serializer = Fail.t();
		} else
			serializer = Fail.t();
		return serializer;
	}

	public <T> Serializer<T> autoFields(Class<T> clazz) {
		Pair<Field, ?>[] pairs = Read //
				.from(inspect.fields(clazz)) //
				.map2(field -> auto_(field.getGenericType())) //
				.toArray();

		Streamlet<Constructor<?>> ctors = Read.from(clazz.getDeclaredConstructors());
		Constructor<?> defaultCtor = ctors.filter(ctor -> ctor.getParameterCount() == 0).first();
		Constructor<?> immutableCtor = ctors.min((c0, c1) -> -Integer.compare(c0.getParameterCount(), c1.getParameterCount()));
		immutableCtor.setAccessible(true);

		return new Serializer<>() {
			public T read(DataInput_ dataInput) throws IOException {
				return Rethrow.ex(() -> {
					Object object;
					if (defaultCtor != null) {
						object = defaultCtor.newInstance();
						for (var pair : pairs)
							pair.t0.set(object, ((Serializer<?>) pair.t1).read(dataInput));
					} else {
						var ps = new Object[immutableCtor.getParameterCount()];
						for (var i = 0; i < ps.length; i++) {
							Pair<Field, ?> pair = pairs[i];
							ps[i] = ((Serializer<?>) pair.t1).read(dataInput);
						}
						object = immutableCtor.newInstance(ps);
					}
					@SuppressWarnings("unchecked")
					var t = (T) object;
					return t;
				});
			}

			public void write(DataOutput_ dataOutput, T t) throws IOException {
				for (var pair : pairs) {
					@SuppressWarnings("unchecked")
					Serializer<Object> serializer1 = (Serializer<Object>) pair.t1;
					serializer1.write(dataOutput, Rethrow.ex(() -> pair.t0.get(t)));
				}

			}
		};
	}

	public Serializer<Bytes> variableLengthBytes = new Serializer<>() {
		public Bytes read(DataInput_ dataInput) throws IOException {
			var length = dataInput.readInt();
			var bs = new byte[length];
			dataInput.readFully(bs);
			return Bytes.of(bs);
		}

		public void write(DataOutput_ dataOutput, Bytes bytes) throws IOException {
			dataOutput.writeInt(bytes.size());
			dataOutput.writeBytes(bytes);
		}
	};

	public Serializer<String> variableLengthString = new Serializer<>() {
		public String read(DataInput_ dataInput) throws IOException {
			return dataInput.readUTF();
		}

		public void write(DataOutput_ dataOutput, String value) throws IOException {
			dataOutput.writeUTF(value);
		}
	};

	public interface Serializer<V> {
		public V read(DataInput_ dataInput) throws IOException;

		public void write(DataOutput_ dataOutput, V value) throws IOException;
	}

	public <T> Serializer<T[]> array(Class<T> clazz, Serializer<T> serializer) {
		return new Serializer<>() {
			public T[] read(DataInput_ dataInput) throws IOException {
				var size = int_.read(dataInput);
				var array = Array_.newArray(clazz, size);
				for (var i = 0; i < size; i++)
					array[i] = serializer.read(dataInput);
				return array;
			}

			public void write(DataOutput_ dataOutput, T[] array) throws IOException {
				int_.write(dataOutput, array.length);
				for (var t : array)
					serializer.write(dataOutput, t);
			}
		};
	}

	public Serializer<float[]> vector = new Serializer<>() {
		public float[] read(DataInput_ dataInput) throws IOException {
			var size = int_.read(dataInput);
			var array = new float[size];
			for (var i = 0; i < size; i++)
				array[i] = dataInput.readFloat();
			return array;
		}

		public void write(DataOutput_ dataOutput, float[] array) throws IOException {
			int_.write(dataOutput, array.length);
			for (var f : array)
				dataOutput.writeFloat(f);
		}
	};

	public Serializer<int[]> arrayOfInts = new Serializer<>() {
		public int[] read(DataInput_ dataInput) throws IOException {
			var size = int_.read(dataInput);
			var array = new int[size];
			for (var i = 0; i < size; i++)
				array[i] = dataInput.readInt();
			return array;
		}

		public void write(DataOutput_ dataOutput, int[] array) throws IOException {
			int_.write(dataOutput, array.length);
			for (var i : array)
				dataOutput.writeInt(i);
		}
	};

	/**
	 * Serialize bytes.
	 *
	 * Size = length
	 */
	public Serializer<Bytes> bytes(int length) {
		return new Serializer<>() {
			public Bytes read(DataInput_ dataInput) throws IOException {
				var bs = new byte[length];
				dataInput.readFully(bs);
				return Bytes.of(bs);
			}

			public void write(DataOutput_ dataOutput, Bytes bytes) throws IOException {
				dataOutput.writeBytes(bytes);
				var i = bytes.size();
				while (i < length) {
					int i1 = min(i + zeroes.length, length);
					dataOutput.write(zeroes, 0, i1 - i);
					i = i1;
				}
			}
		};
	}

	public <T> Serializer<Collection<T>> collection(Serializer<T> serializer) {
		return new Serializer<>() {
			public Collection<T> read(DataInput_ dataInput) throws IOException {
				var size = int_.read(dataInput);
				var list = new ArrayList<T>();
				for (var i = 0; i < size; i++)
					list.add(serializer.read(dataInput));
				return list;
			}

			public void write(DataOutput_ dataOutput, Collection<T> list) throws IOException {
				int_.write(dataOutput, list.size());
				for (var t : list)
					serializer.write(dataOutput, t);
			}
		};
	}

	public Serializer<Extent> extent() {
		return new Serializer<>() {
			public Extent read(DataInput_ dataInput) throws IOException {
				var start = dataInput.readInt();
				var end = dataInput.readInt();
				return new Extent(start, end);
			}

			public void write(DataOutput_ dataOutput, Extent value) throws IOException {
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
	public <T> Serializer<List<T>> list(Serializer<T> serializer) {
		return new Serializer<>() {
			public List<T> read(DataInput_ dataInput) throws IOException {
				var size = int_.read(dataInput);
				var list = new ArrayList<T>();
				for (var i = 0; i < size; i++)
					list.add(serializer.read(dataInput));
				return list;
			}

			public void write(DataOutput_ dataOutput, List<T> list) throws IOException {
				int_.write(dataOutput, list.size());
				for (var t : list)
					serializer.write(dataOutput, t);
			}
		};
	}

	public <K, V> Serializer<Map<K, V>> map(Serializer<K> ks, Serializer<V> vs) {
		return map_(ks, vs);
	}

	public <V> Serializer<Map<String, V>> mapOfString(Serializer<V> vs) {
		return map_(variableLengthString, vs);
	}

	/**
	 * Serializes a nullable value.
	 *
	 * Size = 1 + size of serializer<T>
	 */
	public <T> Serializer<T> nullable(Serializer<T> serializer) {
		return new Serializer<>() {
			public T read(DataInput_ dataInput) throws IOException {
				return boolean_.read(dataInput) ? serializer.read(dataInput) : null;
			}

			@Override
			public void write(DataOutput_ dataOutput, T value) throws IOException {
				boolean isNotNull = value != null;
				boolean_.write(dataOutput, isNotNull);
				if (isNotNull)
					serializer.write(dataOutput, value);
			}
		};
	}

	public <T0, T1> Serializer<Pair<T0, T1>> pair(Serializer<T0> serializer0, Serializer<T1> serializer1) {
		return new Serializer<>() {
			public Pair<T0, T1> read(DataInput_ dataInput) throws IOException {
				var t0 = serializer0.read(dataInput);
				var t1 = serializer1.read(dataInput);
				return Pair.of(t0, t1);
			}

			public void write(DataOutput_ dataOutput, Pair<T0, T1> pair) throws IOException {
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
	public Serializer<String> string(int length) {
		return new Serializer<>() {
			public String read(DataInput_ dataInput) throws IOException {
				var bs = new byte[length];
				var l = dataInput.readInt();
				dataInput.readFully(bs);
				return To.string(bs).substring(0, l);
			}

			public void write(DataOutput_ dataOutput, String value) throws IOException {
				var bs = Arrays.copyOf(value.getBytes(Constants.charset), length);
				dataOutput.writeInt(value.length());
				dataOutput.write(bs);
			}
		};
	}

	public <T> Serializer<T> verify(Object o, Serializer<T> serializer) {
		var c = o.hashCode();

		return new Serializer<>() {
			public T read(DataInput_ dataInput) throws IOException {
				return dataInput.readInt() == c ? serializer.read(dataInput) : Fail.t();
			}

			public void write(DataOutput_ dataOutput, T value) throws IOException {
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
	private Serializer<Boolean> boolean_() {
		return new Serializer<>() {
			public Boolean read(DataInput_ dataInput) throws IOException {
				return dataInput.readByte() == -1;
			}

			public void write(DataOutput_ dataOutput, Boolean value) throws IOException {
				dataOutput.writeByte(value ? -1 : 0);
			}
		};
	}

	/**
	 * Serializes an double.
	 *
	 * Size = 8
	 */
	private Serializer<Double> double_() {
		return new Serializer<>() {
			public Double read(DataInput_ dataInput) throws IOException {
				return dataInput.readDouble();
			}

			public void write(DataOutput_ dataOutput, Double value) throws IOException {
				dataOutput.writeDouble(value);
			}
		};
	}

	/**
	 * Serializes an float.
	 *
	 * Size = 4
	 */
	private Serializer<Float> float_() {
		return new Serializer<>() {
			public Float read(DataInput_ dataInput) throws IOException {
				return dataInput.readFloat();
			}

			public void write(DataOutput_ dataOutput, Float value) throws IOException {
				dataOutput.writeFloat(value);
			}
		};
	}

	/**
	 * Serializes an integer.
	 *
	 * Size = 4
	 */
	private Serializer<Integer> int_() {
		return new Serializer<>() {
			public Integer read(DataInput_ dataInput) throws IOException {
				return dataInput.readInt();
			}

			public void write(DataOutput_ dataOutput, Integer value) throws IOException {
				dataOutput.writeInt(value);
			}
		};
	}

	private <K, V> Serializer<Map<K, V>> map_(Serializer<K> ks, Serializer<V> vs) {
		return new Serializer<>() {
			public Map<K, V> read(DataInput_ dataInput) throws IOException {
				var size = int_.read(dataInput);
				var map = new HashMap<K, V>();
				for (var i = 0; i < size; i++) {
					var k = ks.read(dataInput);
					var v = vs.read(dataInput);
					map.put(k, v);
				}
				return map;
			}

			public void write(DataOutput_ dataOutput, Map<K, V> map) throws IOException {
				int_.write(dataOutput, map.size());
				for (var e : map.entrySet()) {
					ks.write(dataOutput, e.getKey());
					vs.write(dataOutput, e.getValue());
				}
			}
		};
	}

	private <T> Serializer<T> poly(Class<T> interface_) {
		return new Serializer<>() {
			public T read(DataInput_ dataInput) throws IOException {
				var c = Rethrow.ex(() -> Class.forName(dataInput.readUTF()));
				if (interface_.isAssignableFrom(c)) {
					@SuppressWarnings("unchecked")
					var t = (T) auto(c).read(dataInput);
					return t;
				} else
					return Fail.t(c.getSimpleName() + " does not implement " + interface_.getSimpleName());
			}

			public void write(DataOutput_ dataOutput, T t) throws IOException {
				@SuppressWarnings("unchecked")
				var c = (Class<Object>) t.getClass();
				if (interface_.isAssignableFrom(c)) {
					dataOutput.writeUTF(c.getName());
					auto(c).write(dataOutput, t);
				} else
					Fail.t(c.getSimpleName() + " does not implement " + interface_.getSimpleName());
			}
		};
	}

}
