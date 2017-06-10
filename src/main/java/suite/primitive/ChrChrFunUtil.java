package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.ChrChrPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.ChrPrimitives.ChrPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class ChrChrFunUtil {

	public static ChrChrSource append(char key, char value, ChrChrSource source) {
		return new ChrChrSource() {
			private boolean isAppended = false;

			public boolean source2(ChrChrPair pair) {
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

	public static <V> Source<ChrChrSource> chunk(int n, ChrChrSource source) {
		return new Source<ChrChrSource>() {
			private ChrChrPair pair;
			private boolean isAvail;
			private int i;
			private ChrChrSource source_ = pair1 -> {
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

			public ChrChrSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> ChrChrSource concat(Source<ChrChrSource> source) {
		return new ChrChrSource() {
			private ChrChrSource source2 = nullSource();

			public boolean source2(ChrChrPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static ChrChrSource cons(char key, char value, ChrChrSource source2) {
		return new ChrChrSource() {
			private boolean isFirst = true;

			public boolean source2(ChrChrPair pair) {
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

	public static ChrChrSource filter(ChrChrPredicate fun0, ChrChrSource source2) {
		ChrChrPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> ChrChrSource filterKey(ChrPredicate_ fun0, ChrChrSource source2) {
		ChrPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static ChrChrSource filterValue(ChrPredicate_ fun0, ChrChrSource source2) {
		ChrPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, ChrChrPair>, R> fun0, R init, ChrChrSource source2) {
		Fun<Pair<R, ChrChrPair>, R> fun1 = Rethrow.fun(fun0);
		ChrChrPair pair = ChrChrPair.of((char) 0, (char) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(ChrChrPredicate pred0, ChrChrSource source2) {
		ChrChrPredicate pred1 = pred0.rethrow();
		ChrChrPair pair = ChrChrPair.of((char) 0, (char) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(ChrChrPredicate pred0, ChrChrSource source2) {
		ChrChrPredicate pred1 = pred0.rethrow();
		ChrChrPair pair = ChrChrPair.of((char) 0, (char) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<ChrChrPair> iterator(ChrChrSource source2) {
		return new Iterator<ChrChrPair>() {
			private ChrChrPair next = null;

			public boolean hasNext() {
				if (next == null) {
					ChrChrPair next1 = ChrChrPair.of((char) 0, (char) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public ChrChrPair next() {
				ChrChrPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<ChrChrPair> iter(ChrChrSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(ChrChr_Obj<T> fun0, ChrChrSource source2) {
		ChrChr_Obj<T> fun1 = fun0.rethrow();
		ChrChrPair pair = ChrChrPair.of((char) 0, (char) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(ChrChr_Obj<K1> kf0, ChrChr_Obj<V1> vf0, ChrChrSource source2) {
		ChrChr_Obj<K1> kf1 = kf0.rethrow();
		ChrChr_Obj<V1> vf1 = vf0.rethrow();
		ChrChrPair pair1 = ChrChrPair.of((char) 0, (char) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static ChrChrSource mapChrChr(ChrChr_Chr kf0, ChrChr_Chr vf0, ChrChrSource source2) {
		ChrChr_Chr kf1 = kf0.rethrow();
		ChrChr_Chr vf1 = vf0.rethrow();
		ChrChrPair pair1 = ChrChrPair.of((char) 0, (char) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(ChrChr_Obj<T> fun0, ChrChrSource source) {
		ChrChr_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				ChrChrPair pair = ChrChrPair.of((char) 0, (char) 0);
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

	public static <V> ChrChrSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<ChrChrSource> split(ChrChrPredicate fun0, ChrChrSource source2) {
		ChrChrPredicate fun1 = fun0.rethrow();
		return new Source<ChrChrSource>() {
			private ChrChrPair pair = ChrChrPair.of((char) 0, (char) 0);
			private boolean isAvailable;
			private ChrChrSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public ChrChrSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> ChrChrSource suck(Sink<Sink<ChrChrPair>> fun) {
		NullableSyncQueue<ChrChrPair> queue = new NullableSyncQueue<>();
		Sink<ChrChrPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				ChrChrPair p = queue.take();
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
