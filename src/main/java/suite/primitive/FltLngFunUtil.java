package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.FltLngPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.FltPredicate.FltPredicate_;
import suite.primitive.LngPredicate.LngPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class FltLngFunUtil {

	public static FltLngSource append(float key, long value, FltLngSource source) {
		return new FltLngSource() {
			private boolean isAppended = false;

			public boolean source2(FltLngPair pair) {
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

	public static <V> Source<FltLngSource> chunk(int n, FltLngSource source) {
		return new Source<FltLngSource>() {
			private FltLngPair pair;
			private boolean isAvail;
			private int i;
			private FltLngSource source_ = pair1 -> {
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

			public FltLngSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> FltLngSource concat(Source<FltLngSource> source) {
		return new FltLngSource() {
			private FltLngSource source2 = nullSource();

			public boolean source2(FltLngPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static FltLngSource cons(float key, long value, FltLngSource source2) {
		return new FltLngSource() {
			private boolean isFirst = true;

			public boolean source2(FltLngPair pair) {
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

	public static FltLngSource filter(FltLngPredicate fun0, FltLngSource source2) {
		FltLngPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> FltLngSource filterKey(FltPredicate_ fun0, FltLngSource source2) {
		FltPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static FltLngSource filterValue(LngPredicate_ fun0, FltLngSource source2) {
		LngPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, FltLngPair>, R> fun0, R init, FltLngSource source2) {
		Fun<Pair<R, FltLngPair>, R> fun1 = Rethrow.fun(fun0);
		FltLngPair pair = FltLngPair.of((float) 0, (long) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(FltLngPredicate pred0, FltLngSource source2) {
		FltLngPredicate pred1 = pred0.rethrow();
		FltLngPair pair = FltLngPair.of((float) 0, (long) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(FltLngPredicate pred0, FltLngSource source2) {
		FltLngPredicate pred1 = pred0.rethrow();
		FltLngPair pair = FltLngPair.of((float) 0, (long) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<FltLngPair> iterator(FltLngSource source2) {
		return new Iterator<FltLngPair>() {
			private FltLngPair next = null;

			public boolean hasNext() {
				if (next == null) {
					FltLngPair next1 = FltLngPair.of((float) 0, (long) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public FltLngPair next() {
				FltLngPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<FltLngPair> iter(FltLngSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(FltLng_Obj<T> fun0, FltLngSource source2) {
		FltLng_Obj<T> fun1 = fun0.rethrow();
		FltLngPair pair = FltLngPair.of((float) 0, (long) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(FltLng_Obj<K1> kf0, FltLng_Obj<V1> vf0, FltLngSource source2) {
		FltLng_Obj<K1> kf1 = kf0.rethrow();
		FltLng_Obj<V1> vf1 = vf0.rethrow();
		FltLngPair pair1 = FltLngPair.of((float) 0, (long) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static FltLngSource mapFltLng(FltLng_Flt kf0, FltLng_Lng vf0, FltLngSource source2) {
		FltLng_Flt kf1 = kf0.rethrow();
		FltLng_Lng vf1 = vf0.rethrow();
		FltLngPair pair1 = FltLngPair.of((float) 0, (long) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(FltLng_Obj<T> fun0, FltLngSource source) {
		FltLng_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				FltLngPair pair = FltLngPair.of((float) 0, (long) 0);
				T t1 = null;
				while (source.source2(pair))
					if ((t1 = fun1.apply(pair.t0, pair.t1)) != null)
						return t1;
				return null;
			}
		};
	}

	public static <I> Sink<I> nullSink() {
		return i -> {
		};
	}

	public static <V> FltLngSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<FltLngSource> split(FltLngPredicate fun0, FltLngSource source2) {
		FltLngPredicate fun1 = fun0.rethrow();
		return new Source<FltLngSource>() {
			private FltLngPair pair = FltLngPair.of((float) 0, (long) 0);
			private boolean isAvailable;
			private FltLngSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public FltLngSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> FltLngSource suck(Sink<Sink<FltLngPair>> fun) {
		NullableSyncQueue<FltLngPair> queue = new NullableSyncQueue<>();
		Sink<FltLngPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				FltLngPair p = queue.take();
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
