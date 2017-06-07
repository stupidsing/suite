package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.DblDblPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.DblPredicate.DblPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class DblDblFunUtil {

	public static DblDblSource append(double key, double value, DblDblSource source) {
		return new DblDblSource() {
			private boolean isAppended = false;

			public boolean source2(DblDblPair pair) {
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

	public static <V> Source<DblDblSource> chunk(int n, DblDblSource source) {
		return new Source<DblDblSource>() {
			private DblDblPair pair;
			private boolean isAvail;
			private int i;
			private DblDblSource source_ = pair1 -> {
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

			public DblDblSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> DblDblSource concat(Source<DblDblSource> source) {
		return new DblDblSource() {
			private DblDblSource source2 = nullSource();

			public boolean source2(DblDblPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static DblDblSource cons(double key, double value, DblDblSource source2) {
		return new DblDblSource() {
			private boolean isFirst = true;

			public boolean source2(DblDblPair pair) {
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

	public static DblDblSource filter(DblDblPredicate fun0, DblDblSource source2) {
		DblDblPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> DblDblSource filterKey(DblPredicate_ fun0, DblDblSource source2) {
		DblPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static DblDblSource filterValue(DblPredicate_ fun0, DblDblSource source2) {
		DblPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, DblDblPair>, R> fun0, R init, DblDblSource source2) {
		Fun<Pair<R, DblDblPair>, R> fun1 = Rethrow.fun(fun0);
		DblDblPair pair = DblDblPair.of((double) 0, (double) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(DblDblPredicate pred0, DblDblSource source2) {
		DblDblPredicate pred1 = pred0.rethrow();
		DblDblPair pair = DblDblPair.of((double) 0, (double) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(DblDblPredicate pred0, DblDblSource source2) {
		DblDblPredicate pred1 = pred0.rethrow();
		DblDblPair pair = DblDblPair.of((double) 0, (double) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<DblDblPair> iterator(DblDblSource source2) {
		return new Iterator<DblDblPair>() {
			private DblDblPair next = null;

			public boolean hasNext() {
				if (next == null) {
					DblDblPair next1 = DblDblPair.of((double) 0, (double) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public DblDblPair next() {
				DblDblPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<DblDblPair> iter(DblDblSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(DblDbl_Obj<T> fun0, DblDblSource source2) {
		DblDbl_Obj<T> fun1 = fun0.rethrow();
		DblDblPair pair = DblDblPair.of((double) 0, (double) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(DblDbl_Obj<K1> kf0, DblDbl_Obj<V1> vf0, DblDblSource source2) {
		DblDbl_Obj<K1> kf1 = kf0.rethrow();
		DblDbl_Obj<V1> vf1 = vf0.rethrow();
		DblDblPair pair1 = DblDblPair.of((double) 0, (double) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static DblDblSource mapDblDbl(DblDbl_Dbl kf0, DblDbl_Dbl vf0, DblDblSource source2) {
		DblDbl_Dbl kf1 = kf0.rethrow();
		DblDbl_Dbl vf1 = vf0.rethrow();
		DblDblPair pair1 = DblDblPair.of((double) 0, (double) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(DblDbl_Obj<T> fun0, DblDblSource source) {
		DblDbl_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				DblDblPair pair = DblDblPair.of((double) 0, (double) 0);
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

	public static <V> DblDblSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<DblDblSource> split(DblDblPredicate fun0, DblDblSource source2) {
		DblDblPredicate fun1 = fun0.rethrow();
		return new Source<DblDblSource>() {
			private DblDblPair pair = DblDblPair.of((double) 0, (double) 0);
			private boolean isAvailable;
			private DblDblSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public DblDblSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> DblDblSource suck(Sink<Sink<DblDblPair>> fun) {
		NullableSyncQueue<DblDblPair> queue = new NullableSyncQueue<>();
		Sink<DblDblPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				DblDblPair p = queue.take();
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
