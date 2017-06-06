package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.DblChrPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.DblPredicate.DblPredicate_;
import suite.primitive.ChrPredicate.ChrPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class DblChrFunUtil {

	public static DblChrSource append(double key, char value, DblChrSource source) {
		return new DblChrSource() {
			private boolean isAppended = false;

			public boolean source2(DblChrPair pair) {
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

	public static <V> Source<DblChrSource> chunk(int n, DblChrSource source) {
		return new Source<DblChrSource>() {
			private DblChrPair pair;
			private boolean isAvail;
			private int i;
			private DblChrSource source_ = pair1 -> {
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

			public DblChrSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> DblChrSource concat(Source<DblChrSource> source) {
		return new DblChrSource() {
			private DblChrSource source2 = nullSource();

			public boolean source2(DblChrPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static DblChrSource cons(double key, char value, DblChrSource source2) {
		return new DblChrSource() {
			private boolean isFirst = true;

			public boolean source2(DblChrPair pair) {
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

	public static DblChrSource filter(DblChrPredicate fun0, DblChrSource source2) {
		DblChrPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> DblChrSource filterKey(DblPredicate_ fun0, DblChrSource source2) {
		DblPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static DblChrSource filterValue(ChrPredicate_ fun0, DblChrSource source2) {
		ChrPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, DblChrPair>, R> fun0, R init, DblChrSource source2) {
		Fun<Pair<R, DblChrPair>, R> fun1 = Rethrow.fun(fun0);
		DblChrPair pair = DblChrPair.of((double) 0, (char) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(DblChrPredicate pred0, DblChrSource source2) {
		DblChrPredicate pred1 = pred0.rethrow();
		DblChrPair pair = DblChrPair.of((double) 0, (char) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(DblChrPredicate pred0, DblChrSource source2) {
		DblChrPredicate pred1 = pred0.rethrow();
		DblChrPair pair = DblChrPair.of((double) 0, (char) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<DblChrPair> iterator(DblChrSource source2) {
		return new Iterator<DblChrPair>() {
			private DblChrPair next = null;

			public boolean hasNext() {
				if (next == null) {
					DblChrPair next1 = DblChrPair.of((double) 0, (char) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public DblChrPair next() {
				DblChrPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<DblChrPair> iter(DblChrSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(DblChr_Obj<T> fun0, DblChrSource source2) {
		DblChr_Obj<T> fun1 = fun0.rethrow();
		DblChrPair pair = DblChrPair.of((double) 0, (char) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(DblChr_Obj<K1> kf0, DblChr_Obj<V1> vf0, DblChrSource source2) {
		DblChr_Obj<K1> kf1 = kf0.rethrow();
		DblChr_Obj<V1> vf1 = vf0.rethrow();
		DblChrPair pair1 = DblChrPair.of((double) 0, (char) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static DblChrSource mapDblChr(DblChr_Dbl kf0, DblChr_Chr vf0, DblChrSource source2) {
		DblChr_Dbl kf1 = kf0.rethrow();
		DblChr_Chr vf1 = vf0.rethrow();
		DblChrPair pair1 = DblChrPair.of((double) 0, (char) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(DblChr_Obj<T> fun, DblChrSource source) {
		return new Source<T>() {
			public T source() {
				DblChrPair pair = DblChrPair.of((double) 0, (char) 0);
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

	public static <V> DblChrSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<DblChrSource> split(DblChrPredicate fun0, DblChrSource source2) {
		DblChrPredicate fun1 = fun0.rethrow();
		return new Source<DblChrSource>() {
			private DblChrPair pair = DblChrPair.of((double) 0, (char) 0);
			private boolean isAvailable;
			private DblChrSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public DblChrSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> DblChrSource suck(Sink<Sink<DblChrPair>> fun) {
		NullableSyncQueue<DblChrPair> queue = new NullableSyncQueue<>();
		Sink<DblChrPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				DblChrPair p = queue.take();
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
