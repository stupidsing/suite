package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.FltChrPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.FltPredicate.FltPredicate_;
import suite.primitive.ChrPredicate.ChrPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class FltChrFunUtil {

	public static FltChrSource append(float key, char value, FltChrSource source) {
		return new FltChrSource() {
			private boolean isAppended = false;

			public boolean source2(FltChrPair pair) {
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

	public static <V> Source<FltChrSource> chunk(int n, FltChrSource source) {
		return new Source<FltChrSource>() {
			private FltChrPair pair;
			private boolean isAvail;
			private int i;
			private FltChrSource source_ = pair1 -> {
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

			public FltChrSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> FltChrSource concat(Source<FltChrSource> source) {
		return new FltChrSource() {
			private FltChrSource source2 = nullSource();

			public boolean source2(FltChrPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static FltChrSource cons(float key, char value, FltChrSource source2) {
		return new FltChrSource() {
			private boolean isFirst = true;

			public boolean source2(FltChrPair pair) {
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

	public static FltChrSource filter(FltChrPredicate fun0, FltChrSource source2) {
		FltChrPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> FltChrSource filterKey(FltPredicate_ fun0, FltChrSource source2) {
		FltPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static FltChrSource filterValue(ChrPredicate_ fun0, FltChrSource source2) {
		ChrPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, FltChrPair>, R> fun0, R init, FltChrSource source2) {
		Fun<Pair<R, FltChrPair>, R> fun1 = Rethrow.fun(fun0);
		FltChrPair pair = FltChrPair.of((float) 0, (char) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(FltChrPredicate pred0, FltChrSource source2) {
		FltChrPredicate pred1 = pred0.rethrow();
		FltChrPair pair = FltChrPair.of((float) 0, (char) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(FltChrPredicate pred0, FltChrSource source2) {
		FltChrPredicate pred1 = pred0.rethrow();
		FltChrPair pair = FltChrPair.of((float) 0, (char) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<FltChrPair> iterator(FltChrSource source2) {
		return new Iterator<FltChrPair>() {
			private FltChrPair next = null;

			public boolean hasNext() {
				if (next == null) {
					FltChrPair next1 = FltChrPair.of((float) 0, (char) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public FltChrPair next() {
				FltChrPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<FltChrPair> iter(FltChrSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(FltChr_Obj<T> fun0, FltChrSource source2) {
		FltChr_Obj<T> fun1 = fun0.rethrow();
		FltChrPair pair = FltChrPair.of((float) 0, (char) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(FltChr_Obj<K1> kf0, FltChr_Obj<V1> vf0, FltChrSource source2) {
		FltChr_Obj<K1> kf1 = kf0.rethrow();
		FltChr_Obj<V1> vf1 = vf0.rethrow();
		FltChrPair pair1 = FltChrPair.of((float) 0, (char) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static FltChrSource mapFltChr(FltChr_Flt kf0, FltChr_Chr vf0, FltChrSource source2) {
		FltChr_Flt kf1 = kf0.rethrow();
		FltChr_Chr vf1 = vf0.rethrow();
		FltChrPair pair1 = FltChrPair.of((float) 0, (char) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(FltChr_Obj<T> fun0, FltChrSource source) {
		FltChr_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				FltChrPair pair = FltChrPair.of((float) 0, (char) 0);
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

	public static <V> FltChrSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<FltChrSource> split(FltChrPredicate fun0, FltChrSource source2) {
		FltChrPredicate fun1 = fun0.rethrow();
		return new Source<FltChrSource>() {
			private FltChrPair pair = FltChrPair.of((float) 0, (char) 0);
			private boolean isAvailable;
			private FltChrSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public FltChrSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> FltChrSource suck(Sink<Sink<FltChrPair>> fun) {
		NullableSyncQueue<FltChrPair> queue = new NullableSyncQueue<>();
		Sink<FltChrPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				FltChrPair p = queue.take();
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
