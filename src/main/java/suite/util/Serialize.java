package suite.util;

import static suite.util.Friends.min;

import java.io.IOException;
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
import suite.primitive.Bytes;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;

/**
 * Defines interface for reading/writing byte buffer. The operation within the
 * same serializer should always put in the same number of bytes.
 */
public class Serialize {

	public Serializer<Boolean> boolean_ = boolean_();
	public Serializer<Double> double_ = ser(SerInput::readDouble, SerOutput::writeDouble); // size
																							// =
																							// 8
	public Serializer<Float> float_ = ser(SerInput::readFloat, SerOutput::writeFloat); // size
																						// =
																						// 4
	public Serializer<Integer> int_ = ser(SerInput::readInt, SerOutput::writeInt); // size
																					// =
																					// 4

	private Inspect inspect;
	private byte[] zeroes = new byte[Constants.bufferSize];

	public Serialize(Inspect inspect) {
		this.inspect = inspect;
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
				var c1 = (Class<Object>) clazz.getComponentType();
				@SuppressWarnings("unchecked")
				var serializer1 = (Serializer<Object>) auto_(c1);
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

		var ctors = Read.from(clazz.getDeclaredConstructors());
		var defaultCtor = ctors.filter(ctor -> ctor.getParameterCount() == 0).first();
		var immutableCtor = ctors.min((c0, c1) -> -Integer.compare(c0.getParameterCount(), c1.getParameterCount()));
		immutableCtor.setAccessible(true);

		return new Serializer<>() {
			public T read(SerInput si) throws IOException {
				return Rethrow.ex(() -> {
					Object object;
					if (defaultCtor != null) {
						object = defaultCtor.newInstance();
						for (var pair : pairs)
							pair.t0.set(object, ((Serializer<?>) pair.t1).read(si));
					} else {
						var ps = new Object[immutableCtor.getParameterCount()];
						for (var i = 0; i < ps.length; i++) {
							Pair<Field, ?> pair = pairs[i];
							ps[i] = ((Serializer<?>) pair.t1).read(si);
						}
						object = immutableCtor.newInstance(ps);
					}
					@SuppressWarnings("unchecked")
					var t = (T) object;
					return t;
				});
			}

			public void write(SerOutput so, T t) throws IOException {
				for (var pair : pairs) {
					@SuppressWarnings("unchecked")
					var serializer1 = (Serializer<Object>) pair.t1;
					serializer1.write(so, Rethrow.ex(() -> pair.t0.get(t)));
				}

			}
		};
	}

	public Serializer<Bytes> variableLengthBytes = new Serializer<>() {
		public Bytes read(SerInput si) throws IOException {
			var length = si.readInt();
			var bs = length < Constants.bufferSize ? new byte[length] : null;
			si.readFully(bs);
			return Bytes.of(bs);
		}

		public void write(SerOutput so, Bytes bytes) throws IOException {
			so.writeInt(bytes.size());
			so.writeBytes(bytes);
		}
	};

	public Serializer<String> variableLengthString = ser(SerInput::readUTF, SerOutput::writeUTF);

	private static <T> Serializer<T> ser(Deser<T> deser, Ser<T> ser) {
		return new Serializer<>() {
			public T read(SerInput si) throws IOException {
				return deser.read(si);
			}

			public void write(SerOutput so, T value) throws IOException {
				ser.write(so, value);
			}
		};
	}

	public interface Deser<V> {
		public V read(SerInput si) throws IOException;
	}

	public interface Ser<V> {
		public void write(SerOutput so, V v) throws IOException;
	}

	public interface Serializer<V> {
		public V read(SerInput si) throws IOException;

		public void write(SerOutput so, V value) throws IOException;
	}

	public <T> Serializer<T[]> array(Class<T> clazz, Serializer<T> serializer) {
		return new Serializer<>() {
			public T[] read(SerInput si) throws IOException {
				var size = int_.read(si);
				var array = Array_.newArray(clazz, size);
				for (var i = 0; i < size; i++)
					array[i] = serializer.read(si);
				return array;
			}

			public void write(SerOutput so, T[] array) throws IOException {
				int_.write(so, array.length);
				for (var t : array)
					serializer.write(so, t);
			}
		};
	}

	public Serializer<float[]> vector = new Serializer<>() {
		public float[] read(SerInput si) throws IOException {
			var size = int_.read(si);
			var array = new float[size];
			for (var i = 0; i < size; i++)
				array[i] = si.readFloat();
			return array;
		}

		public void write(SerOutput so, float[] array) throws IOException {
			int_.write(so, array.length);
			for (var f : array)
				so.writeFloat(f);
		}
	};

	public Serializer<boolean[]> arrayOfBooleans = new Serializer<>() {
		public boolean[] read(SerInput si) throws IOException {
			var length = int_.read(si);
			var array = length < Constants.bufferLimit ? new boolean[length] : null;
			int i1;
			for (var i0 = 0; i0 < length; i0 = i1) {
				i1 = Math.min(i0 + 32, length);
				int m = 1, c = si.readInt();
				for (var i = i0; i < i1; i++, m <<= 1)
					array[i] = (c & m) != 0;
			}
			return array;
		}

		public void write(SerOutput so, boolean[] array) throws IOException {
			int length = array.length;
			int_.write(so, length);
			int i1;
			for (var i0 = 0; i0 < length; i0 = i1) {
				i1 = Math.min(i0 + 32, length);
				int m = 1, c = 0;
				for (var i = i0; i < i1; i++, m <<= 1)
					c |= array[i] ? m : 0;
				so.writeInt(c);
			}
		}
	};

	public Serializer<int[]> arrayOfInts = new Serializer<>() {
		public int[] read(SerInput si) throws IOException {
			var length = int_.read(si);
			var array = length < Constants.bufferLimit ? new int[length] : null;
			for (var i = 0; i < length; i++)
				array[i] = si.readInt();
			return array;
		}

		public void write(SerOutput so, int[] array) throws IOException {
			int_.write(so, array.length);
			for (var i : array)
				so.writeInt(i);
		}
	};

	/**
	 * Serialize bytes.
	 *
	 * Size = length
	 */
	public Serializer<Bytes> bytes(int length) {
		return new Serializer<>() {
			public Bytes read(SerInput si) throws IOException {
				var bs = new byte[length];
				si.readFully(bs);
				return Bytes.of(bs);
			}

			public void write(SerOutput so, Bytes bytes) throws IOException {
				so.writeBytes(bytes);
				var i = bytes.size();
				while (i < length) {
					int i1 = min(i + zeroes.length, length);
					so.write(zeroes, 0, i1 - i);
					i = i1;
				}
			}
		};
	}

	public <T> Serializer<Collection<T>> collection(Serializer<T> serializer) {
		return new Serializer<>() {
			public Collection<T> read(SerInput si) throws IOException {
				var size = int_.read(si);
				var list = size < Constants.bufferLimit ? new ArrayList<T>() : null;
				for (var i = 0; i < size; i++)
					list.add(serializer.read(si));
				return list;
			}

			public void write(SerOutput so, Collection<T> list) throws IOException {
				int_.write(so, list.size());
				for (var t : list)
					serializer.write(so, t);
			}
		};
	}

	public Serializer<Extent> extent() {
		return new Serializer<>() {
			public Extent read(SerInput si) throws IOException {
				var start = si.readInt();
				var end = si.readInt();
				return new Extent(start, end);
			}

			public void write(SerOutput so, Extent value) throws IOException {
				so.writeInt(value.start);
				so.writeInt(value.end);
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
			public List<T> read(SerInput si) throws IOException {
				var size = int_.read(si);
				var list = new ArrayList<T>();
				for (var i = 0; i < size; i++)
					list.add(serializer.read(si));
				return list;
			}

			public void write(SerOutput so, List<T> list) throws IOException {
				int_.write(so, list.size());
				for (var t : list)
					serializer.write(so, t);
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
			public T read(SerInput si) throws IOException {
				return boolean_.read(si) ? serializer.read(si) : null;
			}

			@Override
			public void write(SerOutput so, T value) throws IOException {
				boolean isNotNull = value != null;
				boolean_.write(so, isNotNull);
				if (isNotNull)
					serializer.write(so, value);
			}
		};
	}

	public <T0, T1> Serializer<Pair<T0, T1>> pair(Serializer<T0> serializer0, Serializer<T1> serializer1) {
		return new Serializer<>() {
			public Pair<T0, T1> read(SerInput si) throws IOException {
				var t0 = serializer0.read(si);
				var t1 = serializer1.read(si);
				return Pair.of(t0, t1);
			}

			public void write(SerOutput so, Pair<T0, T1> pair) throws IOException {
				serializer0.write(so, pair.t0);
				serializer1.write(so, pair.t1);
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
			public String read(SerInput si) throws IOException {
				var bs = new byte[length];
				var l = si.readInt();
				si.readFully(bs);
				return To.string(bs).substring(0, l);
			}

			public void write(SerOutput so, String value) throws IOException {
				var bs = Arrays.copyOf(value.getBytes(Constants.charset), length);
				so.writeInt(value.length());
				so.write(bs);
			}
		};
	}

	public <T> Serializer<T> verify(Object o, Serializer<T> serializer) {
		var c = o.hashCode();

		return new Serializer<>() {
			public T read(SerInput si) throws IOException {
				return si.readInt() == c ? serializer.read(si) : Fail.t();
			}

			public void write(SerOutput so, T value) throws IOException {
				so.writeInt(c);
				serializer.write(so, value);
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
			public Boolean read(SerInput si) throws IOException {
				return si.readByte() == -1;
			}

			public void write(SerOutput so, Boolean value) throws IOException {
				so.writeByte(value ? -1 : 0);
			}
		};
	}

	private <K, V> Serializer<Map<K, V>> map_(Serializer<K> ks, Serializer<V> vs) {
		return new Serializer<>() {
			public Map<K, V> read(SerInput si) throws IOException {
				var size = int_.read(si);
				var map = size < Constants.bufferLimit ? new HashMap<K, V>() : null;
				for (var i = 0; i < size; i++) {
					var k = ks.read(si);
					var v = vs.read(si);
					map.put(k, v);
				}
				return map;
			}

			public void write(SerOutput so, Map<K, V> map) throws IOException {
				int_.write(so, map.size());
				for (var e : map.entrySet()) {
					ks.write(so, e.getKey());
					vs.write(so, e.getValue());
				}
			}
		};
	}

	private <T> Serializer<T> poly(Class<T> interface_) {
		return new Serializer<>() {
			public T read(SerInput si) throws IOException {
				var c = Rethrow.ex(() -> Class.forName(si.readUTF()));
				if (interface_.isAssignableFrom(c)) {
					@SuppressWarnings("unchecked")
					var t = (T) auto(c).read(si);
					return t;
				} else
					return Fail.t(c.getSimpleName() + " does not implement " + interface_.getSimpleName());
			}

			public void write(SerOutput so, T t) throws IOException {
				@SuppressWarnings("unchecked")
				var c = (Class<Object>) t.getClass();
				if (interface_.isAssignableFrom(c)) {
					so.writeUTF(c.getName());
					auto(c).write(so, t);
				} else
					Fail.t(c.getSimpleName() + " does not implement " + interface_.getSimpleName());
			}
		};
	}

}
