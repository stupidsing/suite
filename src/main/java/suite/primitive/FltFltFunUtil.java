package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.FltFltPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.FltPrimitives.FltPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class FltFltFunUtil {

	public static FltFltSource append(float key, float value, FltFltSource source) {
		return new FltFltSource() {
			private boolean isAppended = false;

			public boolean source2(FltFltPair pair) {
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

	public static <V> Source<FltFltSource> chunk(int n, FltFltSource source) {
		return new Source<FltFltSource>() {
			private FltFltPair pair;
			private boolean isAvail;
			private int i;
			private FltFltSource source_ = pair1 -> {
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

			public FltFltSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> FltFltSource concat(Source<FltFltSource> source) {
		return new FltFltSource() {
			private FltFltSource source2 = nullSource();

			public boolean source2(FltFltPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static FltFltSource cons(float key, float value, FltFltSource source2) {
		return new FltFltSource() {
			private boolean isFirst = true;

			public boolean source2(FltFltPair pair) {
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

	public static FltFltSource filter(FltFltPredicate fun0, FltFltSource source2) {
		FltFltPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> FltFltSource filterKey(FltPredicate_ fun0, FltFltSource source2) {
		FltPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static FltFltSource filterValue(FltPredicate_ fun0, FltFltSource source2) {
		FltPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, FltFltPair>, R> fun0, R init, FltFltSource source2) {
		Fun<Pair<R, FltFltPair>, R> fun1 = Rethrow.fun(fun0);
		FltFltPair pair = FltFltPair.of((float) 0, (float) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(FltFltPredicate pred0, FltFltSource source2) {
		FltFltPredicate pred1 = pred0.rethrow();
		FltFltPair pair = FltFltPair.of((float) 0, (float) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(FltFltPredicate pred0, FltFltSource source2) {
		FltFltPredicate pred1 = pred0.rethrow();
		FltFltPair pair = FltFltPair.of((float) 0, (float) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<FltFltPair> iterator(FltFltSource source2) {
		return new Iterator<FltFltPair>() {
			private FltFltPair next = null;

			public boolean hasNext() {
				if (next == null) {
					FltFltPair next1 = FltFltPair.of((float) 0, (float) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public FltFltPair next() {
				FltFltPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<FltFltPair> iter(FltFltSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(FltFlt_Obj<T> fun0, FltFltSource source2) {
		FltFlt_Obj<T> fun1 = fun0.rethrow();
		FltFltPair pair = FltFltPair.of((float) 0, (float) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(FltFlt_Obj<K1> kf0, FltFlt_Obj<V1> vf0, FltFltSource source2) {
		FltFlt_Obj<K1> kf1 = kf0.rethrow();
		FltFlt_Obj<V1> vf1 = vf0.rethrow();
		FltFltPair pair1 = FltFltPair.of((float) 0, (float) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static FltFltSource mapFltFlt(FltFlt_Flt kf0, FltFlt_Flt vf0, FltFltSource source2) {
		FltFlt_Flt kf1 = kf0.rethrow();
		FltFlt_Flt vf1 = vf0.rethrow();
		FltFltPair pair1 = FltFltPair.of((float) 0, (float) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(FltFlt_Obj<T> fun0, FltFltSource source) {
		FltFlt_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				FltFltPair pair = FltFltPair.of((float) 0, (float) 0);
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

	public static <V> FltFltSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<FltFltSource> split(FltFltPredicate fun0, FltFltSource source2) {
		FltFltPredicate fun1 = fun0.rethrow();
		return new Source<FltFltSource>() {
			private FltFltPair pair = FltFltPair.of((float) 0, (float) 0);
			private boolean isAvailable;
			private FltFltSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public FltFltSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> FltFltSource suck(Sink<Sink<FltFltPair>> fun) {
		NullableSyncQueue<FltFltPair> queue = new NullableSyncQueue<>();
		Sink<FltFltPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				FltFltPair p = queue.take();
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
