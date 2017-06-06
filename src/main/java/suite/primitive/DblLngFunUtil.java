package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.DblLngPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.DblPredicate.DblPredicate_;
import suite.primitive.LngPredicate.LngPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class DblLngFunUtil {

	public static DblLngSource append(double key, long value, DblLngSource source) {
		return new DblLngSource() {
			private boolean isAppended = false;

			public boolean source2(DblLngPair pair) {
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

	public static <V> Source<DblLngSource> chunk(int n, DblLngSource source) {
		return new Source<DblLngSource>() {
			private DblLngPair pair;
			private boolean isAvail;
			private int i;
			private DblLngSource source_ = pair1 -> {
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

			public DblLngSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> DblLngSource concat(Source<DblLngSource> source) {
		return new DblLngSource() {
			private DblLngSource source2 = nullSource();

			public boolean source2(DblLngPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static DblLngSource cons(double key, long value, DblLngSource source2) {
		return new DblLngSource() {
			private boolean isFirst = true;

			public boolean source2(DblLngPair pair) {
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

	public static DblLngSource filter(DblLngPredicate fun0, DblLngSource source2) {
		DblLngPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> DblLngSource filterKey(DblPredicate_ fun0, DblLngSource source2) {
		DblPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static DblLngSource filterValue(LngPredicate_ fun0, DblLngSource source2) {
		LngPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, DblLngPair>, R> fun0, R init, DblLngSource source2) {
		Fun<Pair<R, DblLngPair>, R> fun1 = Rethrow.fun(fun0);
		DblLngPair pair = DblLngPair.of((double) 0, (long) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(DblLngPredicate pred0, DblLngSource source2) {
		DblLngPredicate pred1 = pred0.rethrow();
		DblLngPair pair = DblLngPair.of((double) 0, (long) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(DblLngPredicate pred0, DblLngSource source2) {
		DblLngPredicate pred1 = pred0.rethrow();
		DblLngPair pair = DblLngPair.of((double) 0, (long) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<DblLngPair> iterator(DblLngSource source2) {
		return new Iterator<DblLngPair>() {
			private DblLngPair next = null;

			public boolean hasNext() {
				if (next == null) {
					DblLngPair next1 = DblLngPair.of((double) 0, (long) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public DblLngPair next() {
				DblLngPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<DblLngPair> iter(DblLngSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(DblLng_Obj<T> fun0, DblLngSource source2) {
		DblLng_Obj<T> fun1 = fun0.rethrow();
		DblLngPair pair = DblLngPair.of((double) 0, (long) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(DblLng_Obj<K1> kf0, DblLng_Obj<V1> vf0, DblLngSource source2) {
		DblLng_Obj<K1> kf1 = kf0.rethrow();
		DblLng_Obj<V1> vf1 = vf0.rethrow();
		DblLngPair pair1 = DblLngPair.of((double) 0, (long) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static DblLngSource mapDblLng(DblLng_Dbl kf0, DblLng_Lng vf0, DblLngSource source2) {
		DblLng_Dbl kf1 = kf0.rethrow();
		DblLng_Lng vf1 = vf0.rethrow();
		DblLngPair pair1 = DblLngPair.of((double) 0, (long) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(DblLng_Obj<T> fun, DblLngSource source) {
		return new Source<T>() {
			public T source() {
				DblLngPair pair = DblLngPair.of((double) 0, (long) 0);
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

	public static <V> DblLngSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<DblLngSource> split(DblLngPredicate fun0, DblLngSource source2) {
		DblLngPredicate fun1 = fun0.rethrow();
		return new Source<DblLngSource>() {
			private DblLngPair pair = DblLngPair.of((double) 0, (long) 0);
			private boolean isAvailable;
			private DblLngSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public DblLngSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> DblLngSource suck(Sink<Sink<DblLngPair>> fun) {
		NullableSyncQueue<DblLngPair> queue = new NullableSyncQueue<>();
		Sink<DblLngPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				DblLngPair p = queue.take();
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
