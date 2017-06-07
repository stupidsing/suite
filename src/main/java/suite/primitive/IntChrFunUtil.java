package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.IntChrPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.IntPredicate.IntPredicate_;
import suite.primitive.ChrPredicate.ChrPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class IntChrFunUtil {

	public static IntChrSource append(int key, char value, IntChrSource source) {
		return new IntChrSource() {
			private boolean isAppended = false;

			public boolean source2(IntChrPair pair) {
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

	public static <V> Source<IntChrSource> chunk(int n, IntChrSource source) {
		return new Source<IntChrSource>() {
			private IntChrPair pair;
			private boolean isAvail;
			private int i;
			private IntChrSource source_ = pair1 -> {
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

			public IntChrSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> IntChrSource concat(Source<IntChrSource> source) {
		return new IntChrSource() {
			private IntChrSource source2 = nullSource();

			public boolean source2(IntChrPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static IntChrSource cons(int key, char value, IntChrSource source2) {
		return new IntChrSource() {
			private boolean isFirst = true;

			public boolean source2(IntChrPair pair) {
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

	public static IntChrSource filter(IntChrPredicate fun0, IntChrSource source2) {
		IntChrPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> IntChrSource filterKey(IntPredicate_ fun0, IntChrSource source2) {
		IntPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static IntChrSource filterValue(ChrPredicate_ fun0, IntChrSource source2) {
		ChrPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, IntChrPair>, R> fun0, R init, IntChrSource source2) {
		Fun<Pair<R, IntChrPair>, R> fun1 = Rethrow.fun(fun0);
		IntChrPair pair = IntChrPair.of((int) 0, (char) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(IntChrPredicate pred0, IntChrSource source2) {
		IntChrPredicate pred1 = pred0.rethrow();
		IntChrPair pair = IntChrPair.of((int) 0, (char) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(IntChrPredicate pred0, IntChrSource source2) {
		IntChrPredicate pred1 = pred0.rethrow();
		IntChrPair pair = IntChrPair.of((int) 0, (char) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<IntChrPair> iterator(IntChrSource source2) {
		return new Iterator<IntChrPair>() {
			private IntChrPair next = null;

			public boolean hasNext() {
				if (next == null) {
					IntChrPair next1 = IntChrPair.of((int) 0, (char) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public IntChrPair next() {
				IntChrPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<IntChrPair> iter(IntChrSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(IntChr_Obj<T> fun0, IntChrSource source2) {
		IntChr_Obj<T> fun1 = fun0.rethrow();
		IntChrPair pair = IntChrPair.of((int) 0, (char) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(IntChr_Obj<K1> kf0, IntChr_Obj<V1> vf0, IntChrSource source2) {
		IntChr_Obj<K1> kf1 = kf0.rethrow();
		IntChr_Obj<V1> vf1 = vf0.rethrow();
		IntChrPair pair1 = IntChrPair.of((int) 0, (char) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static IntChrSource mapIntChr(IntChr_Int kf0, IntChr_Chr vf0, IntChrSource source2) {
		IntChr_Int kf1 = kf0.rethrow();
		IntChr_Chr vf1 = vf0.rethrow();
		IntChrPair pair1 = IntChrPair.of((int) 0, (char) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(IntChr_Obj<T> fun0, IntChrSource source) {
		IntChr_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				IntChrPair pair = IntChrPair.of((int) 0, (char) 0);
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

	public static <V> IntChrSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<IntChrSource> split(IntChrPredicate fun0, IntChrSource source2) {
		IntChrPredicate fun1 = fun0.rethrow();
		return new Source<IntChrSource>() {
			private IntChrPair pair = IntChrPair.of((int) 0, (char) 0);
			private boolean isAvailable;
			private IntChrSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public IntChrSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> IntChrSource suck(Sink<Sink<IntChrPair>> fun) {
		NullableSyncQueue<IntChrPair> queue = new NullableSyncQueue<>();
		Sink<IntChrPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				IntChrPair p = queue.take();
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
