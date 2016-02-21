package suite.util;

import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import suite.adt.Pair;
import suite.os.LogUtil;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;

public class FunUtil2 {

	@FunctionalInterface
	public interface Source2<K, V> {
		public boolean source2(Pair<K, V> pair);
	}

	public static <K, V> Source<Source2<K, V>> chunk(Source2<K, V> source2, int n) {
		return new Source<Source2<K, V>>() {
			private Pair<K, V> pair;
			private boolean isAvail;
			private int i;
			private Source2<K, V> source_ = pair1 -> {
				boolean b = (isAvail = isAvail && source2.source2(pair)) && ++i < n;
				if (b) {
					pair1.t0 = pair.t0;
					pair1.t1 = pair.t1;
				} else
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
		return new Source2<K, V>() {
			private Source2<K, V> source2 = nullSource();

			public boolean source2(Pair<K, V> pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static <K, V> Source2<K, V> cons(K key, V value, Source2<K, V> source2) {
		return new Source2<K, V>() {
			private boolean isFirst = true;

			public boolean source2(Pair<K, V> pair) {
				if (!isFirst)
					return source2.source2(pair);
				else {
					isFirst = false;
					pair.t0 = key;
					pair.t1 = value;
					return true;
				}
			}
		};
	}

	public static <K, V> Source2<K, V> filter(BiPredicate<K, V> fun, Source2<K, V> source2) {
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, Pair<K, V>>, R> fun, R init, Source2<K, V> source2) {
		Pair<K, V> pair = Pair.of(null, null);
		while (source2.source2(pair))
			init = fun.apply(Pair.of(init, pair));
		return init;
	}

	public static <K, V> Iterator<Pair<K, V>> iterator(Source2<K, V> source2) {
		return new Iterator<Pair<K, V>>() {
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
				Pair<K, V> next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <K, V> Iterable<Pair<K, V>> iter(Source2<K, V> source2) {
		return () -> iterator(source2);
	}

	public static <K, V, T> Source<T> map(BiFunction<K, V, T> fun, Source2<K, V> source2) {
		Pair<K, V> pair = Pair.of(null, null);
		return () -> source2.source2(pair) ? fun.apply(pair.t0, pair.t1) : null;
	}

	public static <I> Sink<I> nullSink() {
		return i -> {
		};
	}

	public static <K, V> Source2<K, V> nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static <K, V> Source<Source2<K, V>> split(Source2<K, V> source2, BiPredicate<K, V> fun) {
		return new Source<Source2<K, V>>() {
			private Pair<K, V> pair = Pair.of(null, null);
			private boolean isAvailable;
			private Source2<K, V> source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public Source2<K, V> source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <K, V> Source2<K, V> suck(Sink<Sink<Pair<K, V>>> fun) {
		NullableSynchronousQueue<Pair<K, V>> queue = new NullableSynchronousQueue<>();
		Sink<Pair<K, V>> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Util.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				Pair<K, V> p = queue.take();
				if (p != null) {
					pair.t0 = p.t0;
					pair.t1 = p.t1;
					return true;
				} else
					return false;
			} catch (InterruptedException ex) {
				thread.interrupt();
				throw new RuntimeException(ex);
			}
		};
	}

	private static <T> void enqueue(NullableSynchronousQueue<T> queue, T t) {
		try {
			queue.offer(t);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
		}
	}

}
