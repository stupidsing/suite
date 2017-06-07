package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.IntIntPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.IntPredicate.IntPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class IntIntFunUtil {

	public static IntIntSource append(int key, int value, IntIntSource source) {
		return new IntIntSource() {
			private boolean isAppended = false;

			public boolean source2(IntIntPair pair) {
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

	public static <V> Source<IntIntSource> chunk(int n, IntIntSource source) {
		return new Source<IntIntSource>() {
			private IntIntPair pair;
			private boolean isAvail;
			private int i;
			private IntIntSource source_ = pair1 -> {
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

			public IntIntSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> IntIntSource concat(Source<IntIntSource> source) {
		return new IntIntSource() {
			private IntIntSource source2 = nullSource();

			public boolean source2(IntIntPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static IntIntSource cons(int key, int value, IntIntSource source2) {
		return new IntIntSource() {
			private boolean isFirst = true;

			public boolean source2(IntIntPair pair) {
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

	public static IntIntSource filter(IntIntPredicate fun0, IntIntSource source2) {
		IntIntPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> IntIntSource filterKey(IntPredicate_ fun0, IntIntSource source2) {
		IntPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static IntIntSource filterValue(IntPredicate_ fun0, IntIntSource source2) {
		IntPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, IntIntPair>, R> fun0, R init, IntIntSource source2) {
		Fun<Pair<R, IntIntPair>, R> fun1 = Rethrow.fun(fun0);
		IntIntPair pair = IntIntPair.of((int) 0, (int) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(IntIntPredicate pred0, IntIntSource source2) {
		IntIntPredicate pred1 = pred0.rethrow();
		IntIntPair pair = IntIntPair.of((int) 0, (int) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(IntIntPredicate pred0, IntIntSource source2) {
		IntIntPredicate pred1 = pred0.rethrow();
		IntIntPair pair = IntIntPair.of((int) 0, (int) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<IntIntPair> iterator(IntIntSource source2) {
		return new Iterator<IntIntPair>() {
			private IntIntPair next = null;

			public boolean hasNext() {
				if (next == null) {
					IntIntPair next1 = IntIntPair.of((int) 0, (int) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public IntIntPair next() {
				IntIntPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<IntIntPair> iter(IntIntSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(IntInt_Obj<T> fun0, IntIntSource source2) {
		IntInt_Obj<T> fun1 = fun0.rethrow();
		IntIntPair pair = IntIntPair.of((int) 0, (int) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(IntInt_Obj<K1> kf0, IntInt_Obj<V1> vf0, IntIntSource source2) {
		IntInt_Obj<K1> kf1 = kf0.rethrow();
		IntInt_Obj<V1> vf1 = vf0.rethrow();
		IntIntPair pair1 = IntIntPair.of((int) 0, (int) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static IntIntSource mapIntInt(IntInt_Int kf0, IntInt_Int vf0, IntIntSource source2) {
		IntInt_Int kf1 = kf0.rethrow();
		IntInt_Int vf1 = vf0.rethrow();
		IntIntPair pair1 = IntIntPair.of((int) 0, (int) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(IntInt_Obj<T> fun0, IntIntSource source) {
		IntInt_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				IntIntPair pair = IntIntPair.of((int) 0, (int) 0);
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

	public static <V> IntIntSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<IntIntSource> split(IntIntPredicate fun0, IntIntSource source2) {
		IntIntPredicate fun1 = fun0.rethrow();
		return new Source<IntIntSource>() {
			private IntIntPair pair = IntIntPair.of((int) 0, (int) 0);
			private boolean isAvailable;
			private IntIntSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public IntIntSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> IntIntSource suck(Sink<Sink<IntIntPair>> fun) {
		NullableSyncQueue<IntIntPair> queue = new NullableSyncQueue<>();
		Sink<IntIntPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				IntIntPair p = queue.take();
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
