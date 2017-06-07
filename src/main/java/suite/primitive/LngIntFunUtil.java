package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.LngIntPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.LngPredicate.LngPredicate_;
import suite.primitive.IntPredicate.IntPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class LngIntFunUtil {

	public static LngIntSource append(long key, int value, LngIntSource source) {
		return new LngIntSource() {
			private boolean isAppended = false;

			public boolean source2(LngIntPair pair) {
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

	public static <V> Source<LngIntSource> chunk(int n, LngIntSource source) {
		return new Source<LngIntSource>() {
			private LngIntPair pair;
			private boolean isAvail;
			private int i;
			private LngIntSource source_ = pair1 -> {
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

			public LngIntSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> LngIntSource concat(Source<LngIntSource> source) {
		return new LngIntSource() {
			private LngIntSource source2 = nullSource();

			public boolean source2(LngIntPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static LngIntSource cons(long key, int value, LngIntSource source2) {
		return new LngIntSource() {
			private boolean isFirst = true;

			public boolean source2(LngIntPair pair) {
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

	public static LngIntSource filter(LngIntPredicate fun0, LngIntSource source2) {
		LngIntPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> LngIntSource filterKey(LngPredicate_ fun0, LngIntSource source2) {
		LngPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static LngIntSource filterValue(IntPredicate_ fun0, LngIntSource source2) {
		IntPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, LngIntPair>, R> fun0, R init, LngIntSource source2) {
		Fun<Pair<R, LngIntPair>, R> fun1 = Rethrow.fun(fun0);
		LngIntPair pair = LngIntPair.of((long) 0, (int) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(LngIntPredicate pred0, LngIntSource source2) {
		LngIntPredicate pred1 = pred0.rethrow();
		LngIntPair pair = LngIntPair.of((long) 0, (int) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(LngIntPredicate pred0, LngIntSource source2) {
		LngIntPredicate pred1 = pred0.rethrow();
		LngIntPair pair = LngIntPair.of((long) 0, (int) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<LngIntPair> iterator(LngIntSource source2) {
		return new Iterator<LngIntPair>() {
			private LngIntPair next = null;

			public boolean hasNext() {
				if (next == null) {
					LngIntPair next1 = LngIntPair.of((long) 0, (int) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public LngIntPair next() {
				LngIntPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<LngIntPair> iter(LngIntSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(LngInt_Obj<T> fun0, LngIntSource source2) {
		LngInt_Obj<T> fun1 = fun0.rethrow();
		LngIntPair pair = LngIntPair.of((long) 0, (int) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(LngInt_Obj<K1> kf0, LngInt_Obj<V1> vf0, LngIntSource source2) {
		LngInt_Obj<K1> kf1 = kf0.rethrow();
		LngInt_Obj<V1> vf1 = vf0.rethrow();
		LngIntPair pair1 = LngIntPair.of((long) 0, (int) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static LngIntSource mapLngInt(LngInt_Lng kf0, LngInt_Int vf0, LngIntSource source2) {
		LngInt_Lng kf1 = kf0.rethrow();
		LngInt_Int vf1 = vf0.rethrow();
		LngIntPair pair1 = LngIntPair.of((long) 0, (int) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(LngInt_Obj<T> fun0, LngIntSource source) {
		LngInt_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				LngIntPair pair = LngIntPair.of((long) 0, (int) 0);
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

	public static <V> LngIntSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<LngIntSource> split(LngIntPredicate fun0, LngIntSource source2) {
		LngIntPredicate fun1 = fun0.rethrow();
		return new Source<LngIntSource>() {
			private LngIntPair pair = LngIntPair.of((long) 0, (int) 0);
			private boolean isAvailable;
			private LngIntSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public LngIntSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> LngIntSource suck(Sink<Sink<LngIntPair>> fun) {
		NullableSyncQueue<LngIntPair> queue = new NullableSyncQueue<>();
		Sink<LngIntPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				LngIntPair p = queue.take();
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
