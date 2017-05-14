package suite.streamlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.ToIntBiFunction;

import suite.adt.IntObjPair;
import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.primitive.BytesUtil;
import suite.primitive.Chars;
import suite.primitive.Chars.CharsBuilder;
import suite.primitive.PrimitiveFun.IntObj_Double;
import suite.primitive.PrimitiveFun.IntObj_Int;
import suite.primitive.PrimitiveFun.ObjObj_Double;
import suite.primitive.PrimitiveFun.Obj_Double;
import suite.primitive.PrimitiveFun.Obj_Float;
import suite.primitive.PrimitiveFun.Obj_Int;
import suite.primitive.PrimitiveSource.IntObjSource;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.To;

public class As {

	public interface Seq<I, O> {
		public O apply(int index, I i);
	}

	public static <T> Fun<Outlet<T>, double[]> arrayOfDoubles(Obj_Double<T> fun) {
		return new Fun<Outlet<T>, double[]>() {
			public double[] apply(Outlet<T> outlet) {
				double results[] = new double[16];
				int size = 0;
				T t;

				while (true) {
					while (size < results.length)
						if ((t = outlet.next()) != null)
							results[size++] = fun.applyAsDouble(t);
						else
							return Arrays.copyOf(results, size);
					results = Arrays.copyOf(results, results.length * 2);
				}
			}
		};
	}

	public static <T> Fun<Outlet<T>, float[]> arrayOfFloats(Obj_Float<T> fun) {
		return new Fun<Outlet<T>, float[]>() {
			public float[] apply(Outlet<T> outlet) {
				float[] results = new float[16];
				int size = 0;
				T t;

				while (true) {
					while (size < results.length)
						if ((t = outlet.next()) != null)
							results[size++] = fun.applyAsFloat(t);
						else
							return Arrays.copyOf(results, size);
					results = Arrays.copyOf(results, results.length * 2);
				}
			}
		};
	}

	public static <T> Fun<Outlet<T>, int[]> arrayOfInts(Obj_Int<T> fun) {
		return new Fun<Outlet<T>, int[]>() {
			public int[] apply(Outlet<T> outlet) {
				int[] results = new int[16];
				int size = 0;
				T t;

				while (true) {
					while (size < results.length)
						if ((t = outlet.next()) != null)
							results[size++] = fun.applyAsInt(t);
						else
							return Arrays.copyOf(results, size);
					results = Arrays.copyOf(results, results.length * 2);
				}
			}
		};
	}

	public static Bytes bytes(Outlet<Bytes> outlet) {
		BytesBuilder bb = new BytesBuilder();
		outlet.forEach(bb::append);
		return bb.toBytes();
	}

	public static Chars chars(Outlet<Chars> outlet) {
		CharsBuilder cb = new CharsBuilder();
		outlet.forEach(cb::append);
		return cb.toChars();
	}

	public static Fun<Outlet<String>, String> conc(String delimiter) {
		return outlet -> {
			StringBuilder sb = new StringBuilder();
			outlet.sink(new Sink<String>() {
				public void sink(String s) {
					sb.append(s);
					sb.append(delimiter);
				}
			});
			return sb.toString();
		};
	}

	public static <T> Streamlet<T> concat(Outlet<Streamlet<T>> outlet) {
		List<T> list = new ArrayList<>();
		outlet.sink(st1 -> st1.sink(list::add));
		return Read.from(list);
	}

	public static Streamlet<String[]> csv(Outlet<Bytes> outlet) {
		return outlet.collect(As::lines_).map(As::csvLine).collect(As::streamlet);
	}

	public static Fun<Outlet<String>, String> joined() {
		return joined("");
	}

	public static Fun<Outlet<String>, String> joined(String delimiter) {
		return joined("", delimiter, "");
	}

	public static Fun<Outlet<String>, String> joined(String before, String delimiter, String after) {
		return outlet -> {
			StringBuilder sb = new StringBuilder();
			sb.append(before);
			outlet.sink(new Sink<String>() {
				private boolean first = true;

				public void sink(String s) {
					if (first)
						first = false;
					else
						sb.append(delimiter);
					sb.append(s);
				}
			});
			sb.append(after);
			return sb.toString();
		};
	}

	public static Outlet<Bytes> lines(Outlet<Bytes> outlet) {
		return lines_(outlet);
	}

	public static <K, V> Map<K, List<V>> listMap(Outlet<Pair<K, V>> outlet) {
		Map<K, List<V>> map = new HashMap<>();
		outlet.sink(pair -> map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1));
		return map;
	}

	public static <K, V> Map<K, V> map(Outlet2<K, V> outlet) {
		Map<K, V> map = new HashMap<>();
		outlet.sink((k, v) -> {
			if (map.put(k, v) != null)
				throw new RuntimeException("duplicate key " + k);
		});
		return map;
	}

	public static <T> Fun<Outlet<T>, Integer> min(Obj_Int<T> fun) {
		return outlet -> {
			Source<T> source = outlet.source();
			T t = source.source();
			int result1;
			if (t != null) {
				int result = fun.applyAsInt(t);
				while ((t = source.source()) != null)
					if ((result1 = fun.applyAsInt(t)) < result)
						result = result1;
				return result;
			} else
				return null;
		};
	}

	public static <K, V> ListMultimap<K, V> multimap(Outlet2<K, List<V>> outlet) {
		return new ListMultimap<>(map(outlet));
	}

	public static <K, V, T> Fun<Outlet2<K, V>, Streamlet<T>> pairMap(BiFunction<K, V, T> fun) {
		return outlet -> new Streamlet<>(() -> outlet.map(fun::apply));
	}

	public static <I, O> Fun<Outlet<I>, Outlet<O>> sequenced(Seq<I, O> seq) {
		return outlet -> Outlet.of(new Source<O>() {
			private int index;

			public O source() {
				I i = outlet.next();
				return i != null ? seq.apply(index++, i) : null;
			}
		});
	}

	public static <K, V> Map<K, Set<V>> setMap(Outlet<Pair<K, V>> outlet) {
		Map<K, Set<V>> map = new HashMap<>();
		outlet.sink(pair -> map.computeIfAbsent(pair.t0, k_ -> new HashSet<>()).add(pair.t1));
		return map;
	}

	public static <T> Streamlet<T> streamlet(Outlet<T> outlet) {
		return Read.from(outlet.toList());
	}

	public static <K, V> Streamlet2<K, V> streamlet2(Outlet2<K, V> outlet) {
		return Read.from2(outlet.toList());
	}

	public static String string(Outlet<Bytes> outlet) {
		return To.string(bytes(outlet));
	}

	public static <T> Obj_Double<Outlet<T>> sumOfDoubles(Obj_Double<T> fun) {
		return outlet -> {
			Source<T> source = outlet.source();
			T t;
			double result = 0d;
			while ((t = source.source()) != null)
				result += fun.applyAsDouble(t);
			return result;
		};
	}

	public static <T> Obj_Double<IntObjOutlet<T>> sumOfDoubles(IntObj_Double<T> fun) {
		return outlet -> {
			IntObjPair<T> pair = IntObjPair.of(0, null);
			IntObjSource<T> source = outlet.source2();
			double result = 0d;
			while (source.source2(pair))
				result += fun.apply(pair.t0, pair.t1);
			return result;
		};
	}

	public static <K, V> Obj_Double<Outlet2<K, V>> sumOfDoubles(ObjObj_Double<K, V> fun) {
		return outlet -> {
			Pair<K, V> pair = Pair.of(null, null);
			Source2<K, V> source = outlet.source2();
			double result = 0d;
			while (source.source2(pair))
				result += fun.applyAsDouble(pair.t0, pair.t1);
			return result;
		};
	}

	public static <T> Obj_Int<Outlet<T>> sumOfInts(Obj_Int<T> fun) {
		return outlet -> {
			Source<T> source = outlet.source();
			T t;
			int result = 0;
			while ((t = source.source()) != null)
				result += fun.applyAsInt(t);
			return result;
		};
	}

	public static <T> Obj_Int<IntObjOutlet<T>> sumOfInts(IntObj_Int<T> fun) {
		return outlet -> {
			IntObjPair<T> pair = IntObjPair.of(0, null);
			IntObjSource<T> source = outlet.source2();
			int result = 0;
			while (source.source2(pair))
				result += fun.apply(pair.t0, pair.t1);
			return result;
		};
	}

	public static <K, V> Obj_Int<Outlet2<K, V>> sumOfInts(ToIntBiFunction<K, V> fun) {
		return outlet -> {
			Pair<K, V> pair = Pair.of(null, null);
			Source2<K, V> source = outlet.source2();
			int result = 0;
			while (source.source2(pair))
				result += fun.applyAsInt(pair.t0, pair.t1);
			return result;
		};
	}

	public static Streamlet<String[]> table(Outlet<Bytes> outlet) {
		return outlet.collect(As::lines_) //
				.map(bytes -> To.string(bytes).split("\t")) //
				.collect(As::streamlet);
	}

	public static Outlet<Chars> decodeUtf8(Outlet<Bytes> bytesOutlet) {
		Source<Bytes> source = bytesOutlet.source();

		return Outlet.of(new Source<Chars>() {
			private BytesBuilder bb = new BytesBuilder();

			public Chars source() {
				Chars chars;
				while ((chars = decode()).size() == 0) {
					Bytes bytes = source.source();
					if (bytes != null)
						bb.append(bytes);
					else if (bb.size() == 0)
						return null;
					else
						throw new RuntimeException();
				}
				return chars;
			}

			private Chars decode() {
				Bytes bytes = bb.toBytes();
				CharsBuilder cb = new CharsBuilder();
				int s = 0;

				while (s < bytes.size()) {
					int b0 = Byte.toUnsignedInt(bytes.get(s++));
					int ch, e;
					if (b0 < 0x80) {
						ch = b0;
						e = s;
					} else if (b0 < 0xE0) {
						ch = b0 & 0x1F;
						e = s + 1;
					} else if (b0 < 0xF0) {
						ch = b0 & 0x0F;
						e = s + 2;
					} else if (b0 < 0xF8) {
						ch = b0 & 0x07;
						e = s + 3;
					} else if (b0 < 0xFC) {
						ch = b0 & 0x03;
						e = s + 4;
					} else if (b0 < 0xFE) {
						ch = b0 & 0x01;
						e = s + 5;
					} else
						throw new RuntimeException();
					if (e <= bytes.size()) {
						while (s < e) {
							int b = Byte.toUnsignedInt(bytes.get(s++));
							if ((b & 0xC0) == 0x80)
								ch = (ch << 6) + (b & 0x3F);
							else
								throw new RuntimeException();
						}
						cb.append((char) ch);
					} else
						break;
				}

				bb = new BytesBuilder();
				bb.append(bytes.range(s));

				return cb.toChars();
			}
		});
	}

	public static Outlet<Bytes> encodeUtf8(Outlet<Chars> charsOutlet) {
		Source<Chars> source = charsOutlet.source();

		return Outlet.of(new Source<Bytes>() {
			public Bytes source() {
				Chars chars = source.source();
				if (chars != null) {
					BytesBuilder bb = new BytesBuilder();
					for (int i = 0; i < chars.size(); i++) {
						char ch = chars.get(i);
						if (ch < 0x80)
							bb.append((byte) ch);
						else if (ch < 0x800) {
							bb.append((byte) (0xC0 + ((ch >> 6) & 0x1F)));
							bb.append((byte) (0x80 + ((ch >> 0) & 0x3F)));
						} else if (ch < 0x10000) {
							bb.append((byte) (0xE0 + ((ch >> 12) & 0x0F)));
							bb.append((byte) (0x80 + ((ch >> 6) & 0x3F)));
							bb.append((byte) (0x80 + ((ch >> 0) & 0x3F)));
						} else {
							bb.append((byte) (0xF0 + ((ch >> 18) & 0x07)));
							bb.append((byte) (0x80 + ((ch >> 12) & 0x3F)));
							bb.append((byte) (0x80 + ((ch >> 6) & 0x3F)));
							bb.append((byte) (0x80 + ((ch >> 0) & 0x3F)));
						}
					}
					return bb.toBytes();
				} else
					return null;
			}
		});
	}

	private static String[] csvLine(Bytes bytes) {
		return csvLine(To.string(bytes));
	}

	private static String[] csvLine(String line) {
		List<String> list = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		int length = line.length();
		int p = 0;
		if (0 < length) {
			while (p < length) {
				char ch = line.charAt(p++);
				if (ch == '"')
					while (p < length)
						if ((ch = line.charAt(p++)) == '"' && p < length && line.charAt(p) == '"') {
							sb.append(ch);
							p++;
						} else if (ch != '"')
							sb.append(ch);
						else
							break;
				else if (ch == ',') {
					list.add(sb.toString());
					sb.setLength(0);
				} else
					sb.append(ch);
			}
			list.add(sb.toString());
		}
		return list.toArray(new String[0]);
	}

	private static Outlet<Bytes> lines_(Outlet<Bytes> outlet) {
		return BytesUtil.split(Bytes.of((byte) 10)).apply(outlet);
	}

}
