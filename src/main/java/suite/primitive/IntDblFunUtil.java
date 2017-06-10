package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.IntDblPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.DblPrimitives.DblPredicate;
import suite.primitive.IntPrimitives.IntPredicate;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class IntDblFunUtil {

	public static IntDblSource append(int key, double value, IntDblSource source) {
		return new IntDblSource() {
			private boolean isAppended = false;

			public boolean source2(IntDblPair pair) {
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

	public static <V> Source<IntDblSource> chunk(int n, IntDblSource source) {
		return new Source<IntDblSource>() {
			private IntDblPair pair;
			private boolean isAvail;
			private int i;
			private IntDblSource source_ = pair1 -> {
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

			public IntDblSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> IntDblSource concat(Source<IntDblSource> source) {
		return new IntDblSource() {
			private IntDblSource source2 = nullSource();

			public boolean source2(IntDblPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static IntDblSource cons(int key, double value, IntDblSource source2) {
		return new IntDblSource() {
			private boolean isFirst = true;

			public boolean source2(IntDblPair pair) {
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

	public static IntDblSource filter(IntDblPredicate fun0, IntDblSource source2) {
		IntDblPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> IntDblSource filterKey(IntPredicate fun0, IntDblSource source2) {
		IntPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static IntDblSource filterValue(DblPredicate fun0, IntDblSource source2) {
		DblPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, IntDblPair>, R> fun0, R init, IntDblSource source2) {
		Fun<Pair<R, IntDblPair>, R> fun1 = Rethrow.fun(fun0);
		IntDblPair pair = IntDblPair.of((int) 0, (double) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(IntDblPredicate pred0, IntDblSource source2) {
		IntDblPredicate pred1 = pred0.rethrow();
		IntDblPair pair = IntDblPair.of((int) 0, (double) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(IntDblPredicate pred0, IntDblSource source2) {
		IntDblPredicate pred1 = pred0.rethrow();
		IntDblPair pair = IntDblPair.of((int) 0, (double) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<IntDblPair> iterator(IntDblSource source2) {
		return new Iterator<IntDblPair>() {
			private IntDblPair next = null;

			public boolean hasNext() {
				if (next == null) {
					IntDblPair next1 = IntDblPair.of((int) 0, (double) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public IntDblPair next() {
				IntDblPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<IntDblPair> iter(IntDblSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(IntDbl_Obj<T> fun0, IntDblSource source2) {
		IntDbl_Obj<T> fun1 = fun0.rethrow();
		IntDblPair pair = IntDblPair.of((int) 0, (double) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(IntDbl_Obj<K1> kf0, IntDbl_Obj<V1> vf0, IntDblSource source2) {
		IntDbl_Obj<K1> kf1 = kf0.rethrow();
		IntDbl_Obj<V1> vf1 = vf0.rethrow();
		IntDblPair pair1 = IntDblPair.of((int) 0, (double) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static IntDblSource mapIntDbl(IntDbl_Int kf0, IntDbl_Dbl vf0, IntDblSource source2) {
		IntDbl_Int kf1 = kf0.rethrow();
		IntDbl_Dbl vf1 = vf0.rethrow();
		IntDblPair pair1 = IntDblPair.of((int) 0, (double) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(IntDbl_Obj<T> fun0, IntDblSource source) {
		IntDbl_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				IntDblPair pair = IntDblPair.of((int) 0, (double) 0);
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

	public static <V> IntDblSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<IntDblSource> split(IntDblPredicate fun0, IntDblSource source2) {
		IntDblPredicate fun1 = fun0.rethrow();
		return new Source<IntDblSource>() {
			private IntDblPair pair = IntDblPair.of((int) 0, (double) 0);
			private boolean isAvailable;
			private IntDblSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public IntDblSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> IntDblSource suck(Sink<Sink<IntDblPair>> fun) {
		NullableSyncQueue<IntDblPair> queue = new NullableSyncQueue<>();
		Sink<IntDblPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				IntDblPair p = queue.take();
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
