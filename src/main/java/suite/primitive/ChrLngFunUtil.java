package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.ChrLngPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.ChrPredicate.ChrPredicate_;
import suite.primitive.LngPredicate.LngPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class ChrLngFunUtil {

	public static ChrLngSource append(char key, long value, ChrLngSource source) {
		return new ChrLngSource() {
			private boolean isAppended = false;

			public boolean source2(ChrLngPair pair) {
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

	public static <V> Source<ChrLngSource> chunk(int n, ChrLngSource source) {
		return new Source<ChrLngSource>() {
			private ChrLngPair pair;
			private boolean isAvail;
			private int i;
			private ChrLngSource source_ = pair1 -> {
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

			public ChrLngSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> ChrLngSource concat(Source<ChrLngSource> source) {
		return new ChrLngSource() {
			private ChrLngSource source2 = nullSource();

			public boolean source2(ChrLngPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static ChrLngSource cons(char key, long value, ChrLngSource source2) {
		return new ChrLngSource() {
			private boolean isFirst = true;

			public boolean source2(ChrLngPair pair) {
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

	public static ChrLngSource filter(ChrLngPredicate fun0, ChrLngSource source2) {
		ChrLngPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> ChrLngSource filterKey(ChrPredicate_ fun0, ChrLngSource source2) {
		ChrPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static ChrLngSource filterValue(LngPredicate_ fun0, ChrLngSource source2) {
		LngPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, ChrLngPair>, R> fun0, R init, ChrLngSource source2) {
		Fun<Pair<R, ChrLngPair>, R> fun1 = Rethrow.fun(fun0);
		ChrLngPair pair = ChrLngPair.of((char) 0, (long) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(ChrLngPredicate pred0, ChrLngSource source2) {
		ChrLngPredicate pred1 = pred0.rethrow();
		ChrLngPair pair = ChrLngPair.of((char) 0, (long) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(ChrLngPredicate pred0, ChrLngSource source2) {
		ChrLngPredicate pred1 = pred0.rethrow();
		ChrLngPair pair = ChrLngPair.of((char) 0, (long) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<ChrLngPair> iterator(ChrLngSource source2) {
		return new Iterator<ChrLngPair>() {
			private ChrLngPair next = null;

			public boolean hasNext() {
				if (next == null) {
					ChrLngPair next1 = ChrLngPair.of((char) 0, (long) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public ChrLngPair next() {
				ChrLngPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<ChrLngPair> iter(ChrLngSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(ChrLng_Obj<T> fun0, ChrLngSource source2) {
		ChrLng_Obj<T> fun1 = fun0.rethrow();
		ChrLngPair pair = ChrLngPair.of((char) 0, (long) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(ChrLng_Obj<K1> kf0, ChrLng_Obj<V1> vf0, ChrLngSource source2) {
		ChrLng_Obj<K1> kf1 = kf0.rethrow();
		ChrLng_Obj<V1> vf1 = vf0.rethrow();
		ChrLngPair pair1 = ChrLngPair.of((char) 0, (long) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static ChrLngSource mapChrLng(ChrLng_Chr kf0, ChrLng_Lng vf0, ChrLngSource source2) {
		ChrLng_Chr kf1 = kf0.rethrow();
		ChrLng_Lng vf1 = vf0.rethrow();
		ChrLngPair pair1 = ChrLngPair.of((char) 0, (long) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(ChrLng_Obj<T> fun, ChrLngSource source) {
		return new Source<T>() {
			public T source() {
				ChrLngPair pair = ChrLngPair.of((char) 0, (long) 0);
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

	public static <V> ChrLngSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<ChrLngSource> split(ChrLngPredicate fun0, ChrLngSource source2) {
		ChrLngPredicate fun1 = fun0.rethrow();
		return new Source<ChrLngSource>() {
			private ChrLngPair pair = ChrLngPair.of((char) 0, (long) 0);
			private boolean isAvailable;
			private ChrLngSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public ChrLngSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> ChrLngSource suck(Sink<Sink<ChrLngPair>> fun) {
		NullableSyncQueue<ChrLngPair> queue = new NullableSyncQueue<>();
		Sink<ChrLngPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				ChrLngPair p = queue.take();
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
