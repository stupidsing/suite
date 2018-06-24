package suite.util;

import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;

public class FunUtil2 {

	public interface BinOp<T> extends BiFun<T, T> {
	}

	public interface FoldOp<I, O> extends Fun2<I, O, O> {
	}

	public interface BiFun<I, O> extends Fun2<I, I, O> {
	}

	public interface Source2<K, V> {
		public boolean source2(Pair<K, V> pair);
	}

	public interface Sink2<K, V> {
		public void sink2(K key, V value);

		public default Sink2<K, V> rethrow() {
			return (k, v) -> {
				try {
					sink2(k, v);
				} catch (Exception ex) {
					Fail.t("for " + k + ", " + v, ex);
				}
			};
		}
	}

	public interface Fun2<X, Y, Z> extends BiFunction<X, Y, Z> {
		public default Fun2<X, Y, Z> rethrow() {
			return (x, y) -> {
				try {
					return apply(x, y);
				} catch (Exception ex) {
					return Fail.t("for " + x + ":" + y, ex);
				}
			};
		}
	}

	public static <K, V> Source<Source2<K, V>> chunk(int n, Source2<K, V> source2) {
		return new Source<>() {
			private Pair<K, V> pair;
			private boolean isAvail;
			private int i;
			private Source2<K, V> source_ = pair1 -> {
				var b = (isAvail = isAvail && source2.source2(pair)) && ++i < n;
				if (b)
					pair1.update(pair.t0, pair.t1);
				else
					i = 0;
				return b;
			};

			{
				isAvail = source2.source2(pair);
			}

			public Source2<K, V> source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <K, V> Source2<K, V> concat(Source<Source2<K, V>> source) {
		return new Source2<>() {
			private Source2<K, V> source2 = nullSource();

			public boolean source2(Pair<K, V> pair) {
				var b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static <K, V> Source2<K, V> cons(K key, V value, Source2<K, V> source2) {
		return new Source2<>() {
			private boolean isFirst = true;

			public boolean source2(Pair<K, V> pair) {
				if (!isFirst)
					return source2.source2(pair);
				else {
					isFirst = false;
					pair.update(key, value);
					return true;
				}
			}
		};
	}

	public static <K, V> Source2<K, V> filter(BiPredicate<K, V> fun0, Source2<K, V> source2) {
		var fun1 = Rethrow.biPredicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <K, V> Source2<K, V> filterKey(Predicate<K> fun0, Source2<K, V> source2) {
		var fun1 = Rethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static <K, V> Source2<K, V> filterValue(Predicate<V> fun0, Source2<K, V> source2) {
		var fun1 = Rethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, Pair<K, V>>, R> fun0, R init, Source2<K, V> source2) {
		var fun1 = fun0.rethrow();
		Pair<K, V> pair = Pair.of(null, null);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static <K, V> boolean isAll(BiPredicate<K, V> pred0, Source2<K, V> source2) {
		var pred1 = Rethrow.biPredicate(pred0);
		Pair<K, V> pair = Pair.of(null, null);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <K, V> boolean isAny(BiPredicate<K, V> pred0, Source2<K, V> source2) {
		var pred1 = Rethrow.biPredicate(pred0);
		Pair<K, V> pair = Pair.of(null, null);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <K, V> Iterator<Pair<K, V>> iterator(Source2<K, V> source2) {
		return new Iterator<>() {
			private Pair<K, V> next = null;

			public boolean hasNext() {
				if (next == null) {
					Pair<K, V> next1 = Pair.of(null, null);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public Pair<K, V> next() {
				var next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <K, V> Iterable<Pair<K, V>> iter(Source2<K, V> source2) {
		return () -> iterator(source2);
	}

	public static <K, V, T> Source<T> map(Fun2<K, V, T> fun0, Source2<K, V> source2) {
		var fun1 = fun0.rethrow();
		Pair<K, V> pair = Pair.of(null, null);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K, V, K1, V1, T> Source2<K1, V1> map2(Fun2<K, V, K1> kf0, Fun2<K, V, V1> vf0, Source2<K, V> source2) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		Pair<K, V> pair1 = Pair.of(null, null);
		return pair -> {
			var b = source2.source2(pair1);
			if (b) {
				pair.update(kf1.apply(pair1.t0, pair1.t1), vf1.apply(pair1.t0, pair1.t1));
			}
			return b;
		};
	}

	public static <I> Sink<I> nullSink() {
		return i -> {
		};
	}

	public static <K, V> Source2<K, V> nullSource() {
		return pair -> false;
	}

	public static <K, V> Source2<K, V> snoc(K key, V value, Source2<K, V> source) {
		return new Source2<>() {
			private boolean isAppended = false;

			public boolean source2(Pair<K, V> pair) {
				if (!isAppended) {
					var b = source.source2(pair);
					if (!b) {
						pair.update(key, value);
						isAppended = true;
					}
					return b;
				} else
					return false;
			}
		};
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must not be
	 * skipped.
	 */
	public static <K, V> Source<Source2<K, V>> split(BiPredicate<K, V> fun0, Source2<K, V> source2) {
		var fun1 = Rethrow.biPredicate(fun0);
		return new Source<>() {
			private Pair<K, V> pair = Pair.of(null, null);
			private boolean isAvailable;
			private Source2<K, V> source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public Source2<K, V> source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and make it into a source.
	 */
	public static <K, V> Source2<K, V> suck(Sink<Sink<Pair<K, V>>> fun) {
		var queue = new NullableSyncQueue<Pair<K, V>>();
		Sink<Pair<K, V>> enqueue = pair -> enqueue(queue, pair);

		var thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				var p = queue.take();
				var b = p != null;
				if (b)
					pair.update(p.t0, p.t1);
				return b;
			} catch (InterruptedException ex) {
				thread.interrupt();
				return Fail.t(ex);
			}
		};
	}

	private static <T> void enqueue(NullableSyncQueue<T> queue, T t) {
		try {
			queue.offer(t);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
		}
	}

}
