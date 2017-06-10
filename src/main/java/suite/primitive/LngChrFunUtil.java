package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.LngChrPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.ChrPrimitives.ChrPredicate;
import suite.primitive.LngPrimitives.LngPredicate;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class LngChrFunUtil {

	public static LngChrSource append(long key, char value, LngChrSource source) {
		return new LngChrSource() {
			private boolean isAppended = false;

			public boolean source2(LngChrPair pair) {
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

	public static <V> Source<LngChrSource> chunk(int n, LngChrSource source) {
		return new Source<LngChrSource>() {
			private LngChrPair pair;
			private boolean isAvail;
			private int i;
			private LngChrSource source_ = pair1 -> {
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

			public LngChrSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> LngChrSource concat(Source<LngChrSource> source) {
		return new LngChrSource() {
			private LngChrSource source2 = nullSource();

			public boolean source2(LngChrPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static LngChrSource cons(long key, char value, LngChrSource source2) {
		return new LngChrSource() {
			private boolean isFirst = true;

			public boolean source2(LngChrPair pair) {
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

	public static LngChrSource filter(LngChrPredicate fun0, LngChrSource source2) {
		LngChrPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> LngChrSource filterKey(LngPredicate fun0, LngChrSource source2) {
		LngPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static LngChrSource filterValue(ChrPredicate fun0, LngChrSource source2) {
		ChrPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, LngChrPair>, R> fun0, R init, LngChrSource source2) {
		Fun<Pair<R, LngChrPair>, R> fun1 = Rethrow.fun(fun0);
		LngChrPair pair = LngChrPair.of((long) 0, (char) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(LngChrPredicate pred0, LngChrSource source2) {
		LngChrPredicate pred1 = pred0.rethrow();
		LngChrPair pair = LngChrPair.of((long) 0, (char) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(LngChrPredicate pred0, LngChrSource source2) {
		LngChrPredicate pred1 = pred0.rethrow();
		LngChrPair pair = LngChrPair.of((long) 0, (char) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<LngChrPair> iterator(LngChrSource source2) {
		return new Iterator<LngChrPair>() {
			private LngChrPair next = null;

			public boolean hasNext() {
				if (next == null) {
					LngChrPair next1 = LngChrPair.of((long) 0, (char) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public LngChrPair next() {
				LngChrPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<LngChrPair> iter(LngChrSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(LngChr_Obj<T> fun0, LngChrSource source2) {
		LngChr_Obj<T> fun1 = fun0.rethrow();
		LngChrPair pair = LngChrPair.of((long) 0, (char) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(LngChr_Obj<K1> kf0, LngChr_Obj<V1> vf0, LngChrSource source2) {
		LngChr_Obj<K1> kf1 = kf0.rethrow();
		LngChr_Obj<V1> vf1 = vf0.rethrow();
		LngChrPair pair1 = LngChrPair.of((long) 0, (char) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static LngChrSource mapLngChr(LngChr_Lng kf0, LngChr_Chr vf0, LngChrSource source2) {
		LngChr_Lng kf1 = kf0.rethrow();
		LngChr_Chr vf1 = vf0.rethrow();
		LngChrPair pair1 = LngChrPair.of((long) 0, (char) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(LngChr_Obj<T> fun0, LngChrSource source) {
		LngChr_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				LngChrPair pair = LngChrPair.of((long) 0, (char) 0);
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

	public static <V> LngChrSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<LngChrSource> split(LngChrPredicate fun0, LngChrSource source2) {
		LngChrPredicate fun1 = fun0.rethrow();
		return new Source<LngChrSource>() {
			private LngChrPair pair = LngChrPair.of((long) 0, (char) 0);
			private boolean isAvailable;
			private LngChrSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public LngChrSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> LngChrSource suck(Sink<Sink<LngChrPair>> fun) {
		NullableSyncQueue<LngChrPair> queue = new NullableSyncQueue<>();
		Sink<LngChrPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				LngChrPair p = queue.take();
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
