package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.LngFltPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.FltPrimitives.FltPredicate_;
import suite.primitive.LngPrimitives.LngPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class LngFltFunUtil {

	public static LngFltSource append(long key, float value, LngFltSource source) {
		return new LngFltSource() {
			private boolean isAppended = false;

			public boolean source2(LngFltPair pair) {
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

	public static <V> Source<LngFltSource> chunk(int n, LngFltSource source) {
		return new Source<LngFltSource>() {
			private LngFltPair pair;
			private boolean isAvail;
			private int i;
			private LngFltSource source_ = pair1 -> {
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

			public LngFltSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> LngFltSource concat(Source<LngFltSource> source) {
		return new LngFltSource() {
			private LngFltSource source2 = nullSource();

			public boolean source2(LngFltPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static LngFltSource cons(long key, float value, LngFltSource source2) {
		return new LngFltSource() {
			private boolean isFirst = true;

			public boolean source2(LngFltPair pair) {
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

	public static LngFltSource filter(LngFltPredicate fun0, LngFltSource source2) {
		LngFltPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> LngFltSource filterKey(LngPredicate_ fun0, LngFltSource source2) {
		LngPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static LngFltSource filterValue(FltPredicate_ fun0, LngFltSource source2) {
		FltPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, LngFltPair>, R> fun0, R init, LngFltSource source2) {
		Fun<Pair<R, LngFltPair>, R> fun1 = Rethrow.fun(fun0);
		LngFltPair pair = LngFltPair.of((long) 0, (float) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(LngFltPredicate pred0, LngFltSource source2) {
		LngFltPredicate pred1 = pred0.rethrow();
		LngFltPair pair = LngFltPair.of((long) 0, (float) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(LngFltPredicate pred0, LngFltSource source2) {
		LngFltPredicate pred1 = pred0.rethrow();
		LngFltPair pair = LngFltPair.of((long) 0, (float) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<LngFltPair> iterator(LngFltSource source2) {
		return new Iterator<LngFltPair>() {
			private LngFltPair next = null;

			public boolean hasNext() {
				if (next == null) {
					LngFltPair next1 = LngFltPair.of((long) 0, (float) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public LngFltPair next() {
				LngFltPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<LngFltPair> iter(LngFltSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(LngFlt_Obj<T> fun0, LngFltSource source2) {
		LngFlt_Obj<T> fun1 = fun0.rethrow();
		LngFltPair pair = LngFltPair.of((long) 0, (float) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(LngFlt_Obj<K1> kf0, LngFlt_Obj<V1> vf0, LngFltSource source2) {
		LngFlt_Obj<K1> kf1 = kf0.rethrow();
		LngFlt_Obj<V1> vf1 = vf0.rethrow();
		LngFltPair pair1 = LngFltPair.of((long) 0, (float) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static LngFltSource mapLngFlt(LngFlt_Lng kf0, LngFlt_Flt vf0, LngFltSource source2) {
		LngFlt_Lng kf1 = kf0.rethrow();
		LngFlt_Flt vf1 = vf0.rethrow();
		LngFltPair pair1 = LngFltPair.of((long) 0, (float) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(LngFlt_Obj<T> fun0, LngFltSource source) {
		LngFlt_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				LngFltPair pair = LngFltPair.of((long) 0, (float) 0);
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

	public static <V> LngFltSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<LngFltSource> split(LngFltPredicate fun0, LngFltSource source2) {
		LngFltPredicate fun1 = fun0.rethrow();
		return new Source<LngFltSource>() {
			private LngFltPair pair = LngFltPair.of((long) 0, (float) 0);
			private boolean isAvailable;
			private LngFltSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public LngFltSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> LngFltSource suck(Sink<Sink<LngFltPair>> fun) {
		NullableSyncQueue<LngFltPair> queue = new NullableSyncQueue<>();
		Sink<LngFltPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				LngFltPair p = queue.take();
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
