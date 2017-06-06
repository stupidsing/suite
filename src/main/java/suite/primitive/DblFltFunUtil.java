package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.DblFltPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.DblPredicate.DblPredicate_;
import suite.primitive.FltPredicate.FltPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class DblFltFunUtil {

	public static DblFltSource append(double key, float value, DblFltSource source) {
		return new DblFltSource() {
			private boolean isAppended = false;

			public boolean source2(DblFltPair pair) {
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

	public static <V> Source<DblFltSource> chunk(int n, DblFltSource source) {
		return new Source<DblFltSource>() {
			private DblFltPair pair;
			private boolean isAvail;
			private int i;
			private DblFltSource source_ = pair1 -> {
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

			public DblFltSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> DblFltSource concat(Source<DblFltSource> source) {
		return new DblFltSource() {
			private DblFltSource source2 = nullSource();

			public boolean source2(DblFltPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static DblFltSource cons(double key, float value, DblFltSource source2) {
		return new DblFltSource() {
			private boolean isFirst = true;

			public boolean source2(DblFltPair pair) {
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

	public static DblFltSource filter(DblFltPredicate fun0, DblFltSource source2) {
		DblFltPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> DblFltSource filterKey(DblPredicate_ fun0, DblFltSource source2) {
		DblPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static DblFltSource filterValue(FltPredicate_ fun0, DblFltSource source2) {
		FltPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, DblFltPair>, R> fun0, R init, DblFltSource source2) {
		Fun<Pair<R, DblFltPair>, R> fun1 = Rethrow.fun(fun0);
		DblFltPair pair = DblFltPair.of((double) 0, (float) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(DblFltPredicate pred0, DblFltSource source2) {
		DblFltPredicate pred1 = pred0.rethrow();
		DblFltPair pair = DblFltPair.of((double) 0, (float) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(DblFltPredicate pred0, DblFltSource source2) {
		DblFltPredicate pred1 = pred0.rethrow();
		DblFltPair pair = DblFltPair.of((double) 0, (float) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<DblFltPair> iterator(DblFltSource source2) {
		return new Iterator<DblFltPair>() {
			private DblFltPair next = null;

			public boolean hasNext() {
				if (next == null) {
					DblFltPair next1 = DblFltPair.of((double) 0, (float) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public DblFltPair next() {
				DblFltPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<DblFltPair> iter(DblFltSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(DblFlt_Obj<T> fun0, DblFltSource source2) {
		DblFlt_Obj<T> fun1 = fun0.rethrow();
		DblFltPair pair = DblFltPair.of((double) 0, (float) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(DblFlt_Obj<K1> kf0, DblFlt_Obj<V1> vf0, DblFltSource source2) {
		DblFlt_Obj<K1> kf1 = kf0.rethrow();
		DblFlt_Obj<V1> vf1 = vf0.rethrow();
		DblFltPair pair1 = DblFltPair.of((double) 0, (float) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static DblFltSource mapDblFlt(DblFlt_Dbl kf0, DblFlt_Flt vf0, DblFltSource source2) {
		DblFlt_Dbl kf1 = kf0.rethrow();
		DblFlt_Flt vf1 = vf0.rethrow();
		DblFltPair pair1 = DblFltPair.of((double) 0, (float) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(DblFlt_Obj<T> fun, DblFltSource source) {
		return new Source<T>() {
			public T source() {
				DblFltPair pair = DblFltPair.of((double) 0, (float) 0);
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

	public static <V> DblFltSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<DblFltSource> split(DblFltPredicate fun0, DblFltSource source2) {
		DblFltPredicate fun1 = fun0.rethrow();
		return new Source<DblFltSource>() {
			private DblFltPair pair = DblFltPair.of((double) 0, (float) 0);
			private boolean isAvailable;
			private DblFltSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public DblFltSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> DblFltSource suck(Sink<Sink<DblFltPair>> fun) {
		NullableSyncQueue<DblFltPair> queue = new NullableSyncQueue<>();
		Sink<DblFltPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				DblFltPair p = queue.take();
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
