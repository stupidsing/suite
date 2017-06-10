package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.FltDblPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.DblPrimitives.DblPredicate_;
import suite.primitive.FltPrimitives.FltPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class FltDblFunUtil {

	public static FltDblSource append(float key, double value, FltDblSource source) {
		return new FltDblSource() {
			private boolean isAppended = false;

			public boolean source2(FltDblPair pair) {
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

	public static <V> Source<FltDblSource> chunk(int n, FltDblSource source) {
		return new Source<FltDblSource>() {
			private FltDblPair pair;
			private boolean isAvail;
			private int i;
			private FltDblSource source_ = pair1 -> {
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

			public FltDblSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> FltDblSource concat(Source<FltDblSource> source) {
		return new FltDblSource() {
			private FltDblSource source2 = nullSource();

			public boolean source2(FltDblPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static FltDblSource cons(float key, double value, FltDblSource source2) {
		return new FltDblSource() {
			private boolean isFirst = true;

			public boolean source2(FltDblPair pair) {
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

	public static FltDblSource filter(FltDblPredicate fun0, FltDblSource source2) {
		FltDblPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> FltDblSource filterKey(FltPredicate_ fun0, FltDblSource source2) {
		FltPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static FltDblSource filterValue(DblPredicate_ fun0, FltDblSource source2) {
		DblPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, FltDblPair>, R> fun0, R init, FltDblSource source2) {
		Fun<Pair<R, FltDblPair>, R> fun1 = Rethrow.fun(fun0);
		FltDblPair pair = FltDblPair.of((float) 0, (double) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(FltDblPredicate pred0, FltDblSource source2) {
		FltDblPredicate pred1 = pred0.rethrow();
		FltDblPair pair = FltDblPair.of((float) 0, (double) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(FltDblPredicate pred0, FltDblSource source2) {
		FltDblPredicate pred1 = pred0.rethrow();
		FltDblPair pair = FltDblPair.of((float) 0, (double) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<FltDblPair> iterator(FltDblSource source2) {
		return new Iterator<FltDblPair>() {
			private FltDblPair next = null;

			public boolean hasNext() {
				if (next == null) {
					FltDblPair next1 = FltDblPair.of((float) 0, (double) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public FltDblPair next() {
				FltDblPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<FltDblPair> iter(FltDblSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(FltDbl_Obj<T> fun0, FltDblSource source2) {
		FltDbl_Obj<T> fun1 = fun0.rethrow();
		FltDblPair pair = FltDblPair.of((float) 0, (double) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(FltDbl_Obj<K1> kf0, FltDbl_Obj<V1> vf0, FltDblSource source2) {
		FltDbl_Obj<K1> kf1 = kf0.rethrow();
		FltDbl_Obj<V1> vf1 = vf0.rethrow();
		FltDblPair pair1 = FltDblPair.of((float) 0, (double) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static FltDblSource mapFltDbl(FltDbl_Flt kf0, FltDbl_Dbl vf0, FltDblSource source2) {
		FltDbl_Flt kf1 = kf0.rethrow();
		FltDbl_Dbl vf1 = vf0.rethrow();
		FltDblPair pair1 = FltDblPair.of((float) 0, (double) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(FltDbl_Obj<T> fun0, FltDblSource source) {
		FltDbl_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				FltDblPair pair = FltDblPair.of((float) 0, (double) 0);
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

	public static <V> FltDblSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<FltDblSource> split(FltDblPredicate fun0, FltDblSource source2) {
		FltDblPredicate fun1 = fun0.rethrow();
		return new Source<FltDblSource>() {
			private FltDblPair pair = FltDblPair.of((float) 0, (double) 0);
			private boolean isAvailable;
			private FltDblSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public FltDblSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> FltDblSource suck(Sink<Sink<FltDblPair>> fun) {
		NullableSyncQueue<FltDblPair> queue = new NullableSyncQueue<>();
		Sink<FltDblPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				FltDblPair p = queue.take();
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
