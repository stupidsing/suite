package suite.streamlet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

import suite.adt.ListMultimap;
import suite.adt.Pair;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.primitive.Chars;
import suite.primitive.Chars.CharsBuilder;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;

public class As {

	public interface Seq<I, O> {
		public O apply(int index, I i);
	}

	public interface ToFloatFunction<T> {
		public float applyAsFloat(T t);
	}

	public static <T> Fun<Outlet<T>, double[]> arrayOfDoubles(ToDoubleFunction<T> fun) {
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

	public static <T> Fun<Outlet<T>, float[]> arrayOfFloats(ToFloatFunction<T> fun) {
		return new Fun<Outlet<T>, float[]>() {
			public float[] apply(Outlet<T> outlet) {
				float results[] = new float[16];
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

	public static <T> Fun<Outlet<T>, int[]> arrayOfInts(ToIntFunction<T> fun) {
		return new Fun<Outlet<T>, int[]>() {
			public int[] apply(Outlet<T> outlet) {
				int results[] = new int[16];
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

	public static <K, V> Map<K, List<V>> listMap(Outlet<Pair<K, V>> outlet) {
		Map<K, List<V>> map = new HashMap<>();
		outlet.sink(pair -> map.computeIfAbsent(pair.t0, k_ -> new ArrayList<>()).add(pair.t1));
		return map;
	}

	public static <K, V> Map<K, V> map(Outlet2<K, V> outlet) {
		Map<K, V> map = new HashMap<>();
		outlet.sink((k, v) -> {
			if (map.put(k, v) != null)
				throw new RuntimeException("Duplicate key " + k);
		});
		return map;
	}

	public static <T> Fun<Outlet<T>, Integer> min(ToIntFunction<T> fun) {
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
		return outlet -> Outlet.from(new Source<O>() {
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

	public static <T> Fun<Outlet<T>, Integer> sum(ToIntFunction<T> fun) {
		return outlet -> {
			Source<T> source = outlet.source();
			T t = source.source();
			int result = 0;
			while ((t = source.source()) != null)
				result += fun.applyAsInt(t);
			return result;
		};
	}

}
