package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.LngDblPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.DblPrimitives.DblPredicate;
import suite.primitive.LngPrimitives.LngPredicate;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class LngDblFunUtil {

	public static LngDblSource append(long key, double value, LngDblSource source) {
		return new LngDblSource() {
			private boolean isAppended = false;

			public boolean source2(LngDblPair pair) {
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

	public static <V> Source<LngDblSource> chunk(int n, LngDblSource source) {
		return new Source<LngDblSource>() {
			private LngDblPair pair;
			private boolean isAvail;
			private int i;
			private LngDblSource source_ = pair1 -> {
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

			public LngDblSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> LngDblSource concat(Source<LngDblSource> source) {
		return new LngDblSource() {
			private LngDblSource source2 = nullSource();

			public boolean source2(LngDblPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static LngDblSource cons(long key, double value, LngDblSource source2) {
		return new LngDblSource() {
			private boolean isFirst = true;

			public boolean source2(LngDblPair pair) {
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

	public static LngDblSource filter(LngDblPredicate fun0, LngDblSource source2) {
		LngDblPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> LngDblSource filterKey(LngPredicate fun0, LngDblSource source2) {
		LngPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static LngDblSource filterValue(DblPredicate fun0, LngDblSource source2) {
		DblPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, LngDblPair>, R> fun0, R init, LngDblSource source2) {
		Fun<Pair<R, LngDblPair>, R> fun1 = Rethrow.fun(fun0);
		LngDblPair pair = LngDblPair.of((long) 0, (double) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(LngDblPredicate pred0, LngDblSource source2) {
		LngDblPredicate pred1 = pred0.rethrow();
		LngDblPair pair = LngDblPair.of((long) 0, (double) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(LngDblPredicate pred0, LngDblSource source2) {
		LngDblPredicate pred1 = pred0.rethrow();
		LngDblPair pair = LngDblPair.of((long) 0, (double) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<LngDblPair> iterator(LngDblSource source2) {
		return new Iterator<LngDblPair>() {
			private LngDblPair next = null;

			public boolean hasNext() {
				if (next == null) {
					LngDblPair next1 = LngDblPair.of((long) 0, (double) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public LngDblPair next() {
				LngDblPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<LngDblPair> iter(LngDblSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(LngDbl_Obj<T> fun0, LngDblSource source2) {
		LngDbl_Obj<T> fun1 = fun0.rethrow();
		LngDblPair pair = LngDblPair.of((long) 0, (double) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(LngDbl_Obj<K1> kf0, LngDbl_Obj<V1> vf0, LngDblSource source2) {
		LngDbl_Obj<K1> kf1 = kf0.rethrow();
		LngDbl_Obj<V1> vf1 = vf0.rethrow();
		LngDblPair pair1 = LngDblPair.of((long) 0, (double) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static LngDblSource mapLngDbl(LngDbl_Lng kf0, LngDbl_Dbl vf0, LngDblSource source2) {
		LngDbl_Lng kf1 = kf0.rethrow();
		LngDbl_Dbl vf1 = vf0.rethrow();
		LngDblPair pair1 = LngDblPair.of((long) 0, (double) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(LngDbl_Obj<T> fun0, LngDblSource source) {
		LngDbl_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				LngDblPair pair = LngDblPair.of((long) 0, (double) 0);
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

	public static <V> LngDblSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<LngDblSource> split(LngDblPredicate fun0, LngDblSource source2) {
		LngDblPredicate fun1 = fun0.rethrow();
		return new Source<LngDblSource>() {
			private LngDblPair pair = LngDblPair.of((long) 0, (double) 0);
			private boolean isAvailable;
			private LngDblSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public LngDblSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> LngDblSource suck(Sink<Sink<LngDblPair>> fun) {
		NullableSyncQueue<LngDblPair> queue = new NullableSyncQueue<>();
		Sink<LngDblPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				LngDblPair p = queue.take();
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
