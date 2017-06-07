package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.ChrDblPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.ChrPredicate.ChrPredicate_;
import suite.primitive.DblPredicate.DblPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class ChrDblFunUtil {

	public static ChrDblSource append(char key, double value, ChrDblSource source) {
		return new ChrDblSource() {
			private boolean isAppended = false;

			public boolean source2(ChrDblPair pair) {
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

	public static <V> Source<ChrDblSource> chunk(int n, ChrDblSource source) {
		return new Source<ChrDblSource>() {
			private ChrDblPair pair;
			private boolean isAvail;
			private int i;
			private ChrDblSource source_ = pair1 -> {
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

			public ChrDblSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> ChrDblSource concat(Source<ChrDblSource> source) {
		return new ChrDblSource() {
			private ChrDblSource source2 = nullSource();

			public boolean source2(ChrDblPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static ChrDblSource cons(char key, double value, ChrDblSource source2) {
		return new ChrDblSource() {
			private boolean isFirst = true;

			public boolean source2(ChrDblPair pair) {
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

	public static ChrDblSource filter(ChrDblPredicate fun0, ChrDblSource source2) {
		ChrDblPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> ChrDblSource filterKey(ChrPredicate_ fun0, ChrDblSource source2) {
		ChrPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static ChrDblSource filterValue(DblPredicate_ fun0, ChrDblSource source2) {
		DblPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, ChrDblPair>, R> fun0, R init, ChrDblSource source2) {
		Fun<Pair<R, ChrDblPair>, R> fun1 = Rethrow.fun(fun0);
		ChrDblPair pair = ChrDblPair.of((char) 0, (double) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(ChrDblPredicate pred0, ChrDblSource source2) {
		ChrDblPredicate pred1 = pred0.rethrow();
		ChrDblPair pair = ChrDblPair.of((char) 0, (double) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(ChrDblPredicate pred0, ChrDblSource source2) {
		ChrDblPredicate pred1 = pred0.rethrow();
		ChrDblPair pair = ChrDblPair.of((char) 0, (double) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<ChrDblPair> iterator(ChrDblSource source2) {
		return new Iterator<ChrDblPair>() {
			private ChrDblPair next = null;

			public boolean hasNext() {
				if (next == null) {
					ChrDblPair next1 = ChrDblPair.of((char) 0, (double) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public ChrDblPair next() {
				ChrDblPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<ChrDblPair> iter(ChrDblSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(ChrDbl_Obj<T> fun0, ChrDblSource source2) {
		ChrDbl_Obj<T> fun1 = fun0.rethrow();
		ChrDblPair pair = ChrDblPair.of((char) 0, (double) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(ChrDbl_Obj<K1> kf0, ChrDbl_Obj<V1> vf0, ChrDblSource source2) {
		ChrDbl_Obj<K1> kf1 = kf0.rethrow();
		ChrDbl_Obj<V1> vf1 = vf0.rethrow();
		ChrDblPair pair1 = ChrDblPair.of((char) 0, (double) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static ChrDblSource mapChrDbl(ChrDbl_Chr kf0, ChrDbl_Dbl vf0, ChrDblSource source2) {
		ChrDbl_Chr kf1 = kf0.rethrow();
		ChrDbl_Dbl vf1 = vf0.rethrow();
		ChrDblPair pair1 = ChrDblPair.of((char) 0, (double) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(ChrDbl_Obj<T> fun0, ChrDblSource source) {
		ChrDbl_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				ChrDblPair pair = ChrDblPair.of((char) 0, (double) 0);
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

	public static <V> ChrDblSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<ChrDblSource> split(ChrDblPredicate fun0, ChrDblSource source2) {
		ChrDblPredicate fun1 = fun0.rethrow();
		return new Source<ChrDblSource>() {
			private ChrDblPair pair = ChrDblPair.of((char) 0, (double) 0);
			private boolean isAvailable;
			private ChrDblSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public ChrDblSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> ChrDblSource suck(Sink<Sink<ChrDblPair>> fun) {
		NullableSyncQueue<ChrDblPair> queue = new NullableSyncQueue<>();
		Sink<ChrDblPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				ChrDblPair p = queue.take();
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
