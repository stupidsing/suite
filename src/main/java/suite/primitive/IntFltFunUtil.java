package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.IntFltPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.IntPredicate.IntPredicate_;
import suite.primitive.FltPredicate.FltPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class IntFltFunUtil {

	public static IntFltSource append(int key, float value, IntFltSource source) {
		return new IntFltSource() {
			private boolean isAppended = false;

			public boolean source2(IntFltPair pair) {
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

	public static <V> Source<IntFltSource> chunk(int n, IntFltSource source) {
		return new Source<IntFltSource>() {
			private IntFltPair pair;
			private boolean isAvail;
			private int i;
			private IntFltSource source_ = pair1 -> {
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

			public IntFltSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> IntFltSource concat(Source<IntFltSource> source) {
		return new IntFltSource() {
			private IntFltSource source2 = nullSource();

			public boolean source2(IntFltPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static IntFltSource cons(int key, float value, IntFltSource source2) {
		return new IntFltSource() {
			private boolean isFirst = true;

			public boolean source2(IntFltPair pair) {
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

	public static IntFltSource filter(IntFltPredicate fun0, IntFltSource source2) {
		IntFltPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> IntFltSource filterKey(IntPredicate_ fun0, IntFltSource source2) {
		IntPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static IntFltSource filterValue(FltPredicate_ fun0, IntFltSource source2) {
		FltPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, IntFltPair>, R> fun0, R init, IntFltSource source2) {
		Fun<Pair<R, IntFltPair>, R> fun1 = Rethrow.fun(fun0);
		IntFltPair pair = IntFltPair.of((int) 0, (float) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(IntFltPredicate pred0, IntFltSource source2) {
		IntFltPredicate pred1 = pred0.rethrow();
		IntFltPair pair = IntFltPair.of((int) 0, (float) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(IntFltPredicate pred0, IntFltSource source2) {
		IntFltPredicate pred1 = pred0.rethrow();
		IntFltPair pair = IntFltPair.of((int) 0, (float) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<IntFltPair> iterator(IntFltSource source2) {
		return new Iterator<IntFltPair>() {
			private IntFltPair next = null;

			public boolean hasNext() {
				if (next == null) {
					IntFltPair next1 = IntFltPair.of((int) 0, (float) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public IntFltPair next() {
				IntFltPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<IntFltPair> iter(IntFltSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(IntFlt_Obj<T> fun0, IntFltSource source2) {
		IntFlt_Obj<T> fun1 = fun0.rethrow();
		IntFltPair pair = IntFltPair.of((int) 0, (float) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(IntFlt_Obj<K1> kf0, IntFlt_Obj<V1> vf0, IntFltSource source2) {
		IntFlt_Obj<K1> kf1 = kf0.rethrow();
		IntFlt_Obj<V1> vf1 = vf0.rethrow();
		IntFltPair pair1 = IntFltPair.of((int) 0, (float) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static IntFltSource mapIntFlt(IntFlt_Int kf0, IntFlt_Flt vf0, IntFltSource source2) {
		IntFlt_Int kf1 = kf0.rethrow();
		IntFlt_Flt vf1 = vf0.rethrow();
		IntFltPair pair1 = IntFltPair.of((int) 0, (float) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(IntFlt_Obj<T> fun, IntFltSource source) {
		return new Source<T>() {
			public T source() {
				IntFltPair pair = IntFltPair.of((int) 0, (float) 0);
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

	public static <V> IntFltSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<IntFltSource> split(IntFltPredicate fun0, IntFltSource source2) {
		IntFltPredicate fun1 = fun0.rethrow();
		return new Source<IntFltSource>() {
			private IntFltPair pair = IntFltPair.of((int) 0, (float) 0);
			private boolean isAvailable;
			private IntFltSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public IntFltSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> IntFltSource suck(Sink<Sink<IntFltPair>> fun) {
		NullableSyncQueue<IntFltPair> queue = new NullableSyncQueue<>();
		Sink<IntFltPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				IntFltPair p = queue.take();
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
