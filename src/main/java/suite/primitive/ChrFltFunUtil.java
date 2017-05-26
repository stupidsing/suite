package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.ChrFltPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.ChrPredicate.ChrPredicate_;
import suite.primitive.FltPredicate.FltPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class ChrFltFunUtil {

	public static ChrFltSource append(char key, float value, ChrFltSource source) {
		return new ChrFltSource() {
			private boolean isAppended = false;

			public boolean source2(ChrFltPair pair) {
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

	public static <V> Source<ChrFltSource> chunk(int n, ChrFltSource source) {
		return new Source<ChrFltSource>() {
			private ChrFltPair pair;
			private boolean isAvail;
			private int i;
			private ChrFltSource source_ = pair1 -> {
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

			public ChrFltSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> ChrFltSource concat(Source<ChrFltSource> source) {
		return new ChrFltSource() {
			private ChrFltSource source2 = nullSource();

			public boolean source2(ChrFltPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static ChrFltSource cons(char key, float value, ChrFltSource source2) {
		return new ChrFltSource() {
			private boolean isFirst = true;

			public boolean source2(ChrFltPair pair) {
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

	public static ChrFltSource filter(ChrFltPredicate fun0, ChrFltSource source2) {
		ChrFltPredicate fun1 = ChrFltRethrow.chrFltPredicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> ChrFltSource filterKey(ChrPredicate_ fun0, ChrFltSource source2) {
		ChrPredicate_ fun1 = ChrRethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static ChrFltSource filterValue(FltPredicate_ fun0, ChrFltSource source2) {
		FltPredicate_ fun1 = FltRethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, ChrFltPair>, R> fun0, R init, ChrFltSource source2) {
		Fun<Pair<R, ChrFltPair>, R> fun1 = Rethrow.fun(fun0);
		ChrFltPair pair = ChrFltPair.of((char) 0, (float) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(ChrFltPredicate pred0, ChrFltSource source2) {
		ChrFltPredicate pred1 = ChrFltRethrow.chrFltPredicate(pred0);
		ChrFltPair pair = ChrFltPair.of((char) 0, (float) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(ChrFltPredicate pred0, ChrFltSource source2) {
		ChrFltPredicate pred1 = ChrFltRethrow.chrFltPredicate(pred0);
		ChrFltPair pair = ChrFltPair.of((char) 0, (float) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<ChrFltPair> iterator(ChrFltSource source2) {
		return new Iterator<ChrFltPair>() {
			private ChrFltPair next = null;

			public boolean hasNext() {
				if (next == null) {
					ChrFltPair next1 = ChrFltPair.of((char) 0, (float) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public ChrFltPair next() {
				ChrFltPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<ChrFltPair> iter(ChrFltSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(ChrFlt_Obj<T> fun0, ChrFltSource source2) {
		ChrFlt_Obj<T> fun1 = ChrFltRethrow.fun2(fun0);
		ChrFltPair pair = ChrFltPair.of((char) 0, (float) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(ChrFlt_Obj<K1> kf0, ChrFlt_Obj<V1> vf0, ChrFltSource source2) {
		ChrFlt_Obj<K1> kf1 = ChrFltRethrow.fun2(kf0);
		ChrFlt_Obj<V1> vf1 = ChrFltRethrow.fun2(vf0);
		ChrFltPair pair1 = ChrFltPair.of((char) 0, (float) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static ChrFltSource mapChrFlt(ChrFlt_Chr kf0, ChrFlt_Flt vf0, ChrFltSource source2) {
		ChrFlt_Chr kf1 = ChrFlt_ChrRethrow.fun2(kf0);
		ChrFlt_Flt vf1 = ChrFlt_FltRethrow.fun2(vf0);
		ChrFltPair pair1 = ChrFltPair.of((char) 0, (float) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(ChrFlt_Obj<T> fun, ChrFltSource source) {
		return new Source<T>() {
			public T source() {
				ChrFltPair pair = ChrFltPair.of((char) 0, (float) 0);
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

	public static <V> ChrFltSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<ChrFltSource> split(ChrFltPredicate fun0, ChrFltSource source2) {
		ChrFltPredicate fun1 = ChrFltRethrow.chrFltPredicate(fun0);
		return new Source<ChrFltSource>() {
			private ChrFltPair pair = ChrFltPair.of((char) 0, (float) 0);
			private boolean isAvailable;
			private ChrFltSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public ChrFltSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> ChrFltSource suck(Sink<Sink<ChrFltPair>> fun) {
		NullableSyncQueue<ChrFltPair> queue = new NullableSyncQueue<>();
		Sink<ChrFltPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				ChrFltPair p = queue.take();
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
