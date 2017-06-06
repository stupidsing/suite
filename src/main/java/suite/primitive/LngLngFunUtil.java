package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.LngLngPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.LngPredicate.LngPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class LngLngFunUtil {

	public static LngLngSource append(long key, long value, LngLngSource source) {
		return new LngLngSource() {
			private boolean isAppended = false;

			public boolean source2(LngLngPair pair) {
				if (!isAppended) {
					boolean b = source.source2(pair);
					if (!b) {
						pair.t0 = key;
						pair.t1 = value;
						isAppended = true;
					}
					return b;
				} else
					return false;
			}
		};
	}

	public static <V> Source<LngLngSource> chunk(int n, LngLngSource source) {
		return new Source<LngLngSource>() {
			private LngLngPair pair;
			private boolean isAvail;
			private int i;
			private LngLngSource source_ = pair1 -> {
				boolean b = (isAvail = isAvail && source.source2(pair)) && ++i < n;
				if (b) {
					pair1.t0 = pair.t0;
					pair1.t1 = pair.t1;
				} else
					i = 0;
				return b;
			};

			{
				isAvail = source.source2(pair);
			}

			public LngLngSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> LngLngSource concat(Source<LngLngSource> source) {
		return new LngLngSource() {
			private LngLngSource source2 = nullSource();

			public boolean source2(LngLngPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static LngLngSource cons(long key, long value, LngLngSource source2) {
		return new LngLngSource() {
			private boolean isFirst = true;

			public boolean source2(LngLngPair pair) {
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

	public static LngLngSource filter(LngLngPredicate fun0, LngLngSource source2) {
		LngLngPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> LngLngSource filterKey(LngPredicate_ fun0, LngLngSource source2) {
		LngPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static LngLngSource filterValue(LngPredicate_ fun0, LngLngSource source2) {
		LngPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, LngLngPair>, R> fun0, R init, LngLngSource source2) {
		Fun<Pair<R, LngLngPair>, R> fun1 = Rethrow.fun(fun0);
		LngLngPair pair = LngLngPair.of((long) 0, (long) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(LngLngPredicate pred0, LngLngSource source2) {
		LngLngPredicate pred1 = pred0.rethrow();
		LngLngPair pair = LngLngPair.of((long) 0, (long) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(LngLngPredicate pred0, LngLngSource source2) {
		LngLngPredicate pred1 = pred0.rethrow();
		LngLngPair pair = LngLngPair.of((long) 0, (long) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<LngLngPair> iterator(LngLngSource source2) {
		return new Iterator<LngLngPair>() {
			private LngLngPair next = null;

			public boolean hasNext() {
				if (next == null) {
					LngLngPair next1 = LngLngPair.of((long) 0, (long) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public LngLngPair next() {
				LngLngPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<LngLngPair> iter(LngLngSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(LngLng_Obj<T> fun0, LngLngSource source2) {
		LngLng_Obj<T> fun1 = fun0.rethrow();
		LngLngPair pair = LngLngPair.of((long) 0, (long) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(LngLng_Obj<K1> kf0, LngLng_Obj<V1> vf0, LngLngSource source2) {
		LngLng_Obj<K1> kf1 = kf0.rethrow();
		LngLng_Obj<V1> vf1 = vf0.rethrow();
		LngLngPair pair1 = LngLngPair.of((long) 0, (long) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static LngLngSource mapLngLng(LngLng_Lng kf0, LngLng_Lng vf0, LngLngSource source2) {
		LngLng_Lng kf1 = kf0.rethrow();
		LngLng_Lng vf1 = vf0.rethrow();
		LngLngPair pair1 = LngLngPair.of((long) 0, (long) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(LngLng_Obj<T> fun, LngLngSource source) {
		return new Source<T>() {
			public T source() {
				LngLngPair pair = LngLngPair.of((long) 0, (long) 0);
				T t1 = null;
				while (source.source2(pair))
					if ((t1 = fun.apply(pair.t0, pair.t1)) != null)
						return t1;
				return null;
			}
		};
	}

	public static <I> Sink<I> nullSink() {
		return i -> {
		};
	}

	public static <V> LngLngSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<LngLngSource> split(LngLngPredicate fun0, LngLngSource source2) {
		LngLngPredicate fun1 = fun0.rethrow();
		return new Source<LngLngSource>() {
			private LngLngPair pair = LngLngPair.of((long) 0, (long) 0);
			private boolean isAvailable;
			private LngLngSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public LngLngSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> LngLngSource suck(Sink<Sink<LngLngPair>> fun) {
		NullableSyncQueue<LngLngPair> queue = new NullableSyncQueue<>();
		Sink<LngLngPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				LngLngPair p = queue.take();
				boolean b = p != null;
				if (b) {
					pair.t0 = p.t0;
					pair.t1 = p.t1;
				}
				return b;
			} catch (InterruptedException ex) {
				thread.interrupt();
				throw new RuntimeException(ex);
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
