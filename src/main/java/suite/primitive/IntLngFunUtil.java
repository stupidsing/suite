package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.IntLngPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.IntPredicate.IntPredicate_;
import suite.primitive.LngPredicate.LngPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class IntLngFunUtil {

	public static IntLngSource append(int key, long value, IntLngSource source) {
		return new IntLngSource() {
			private boolean isAppended = false;

			public boolean source2(IntLngPair pair) {
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

	public static <V> Source<IntLngSource> chunk(int n, IntLngSource source) {
		return new Source<IntLngSource>() {
			private IntLngPair pair;
			private boolean isAvail;
			private int i;
			private IntLngSource source_ = pair1 -> {
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

			public IntLngSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> IntLngSource concat(Source<IntLngSource> source) {
		return new IntLngSource() {
			private IntLngSource source2 = nullSource();

			public boolean source2(IntLngPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static IntLngSource cons(int key, long value, IntLngSource source2) {
		return new IntLngSource() {
			private boolean isFirst = true;

			public boolean source2(IntLngPair pair) {
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

	public static IntLngSource filter(IntLngPredicate fun0, IntLngSource source2) {
		IntLngPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> IntLngSource filterKey(IntPredicate_ fun0, IntLngSource source2) {
		IntPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static IntLngSource filterValue(LngPredicate_ fun0, IntLngSource source2) {
		LngPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, IntLngPair>, R> fun0, R init, IntLngSource source2) {
		Fun<Pair<R, IntLngPair>, R> fun1 = Rethrow.fun(fun0);
		IntLngPair pair = IntLngPair.of((int) 0, (long) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(IntLngPredicate pred0, IntLngSource source2) {
		IntLngPredicate pred1 = pred0.rethrow();
		IntLngPair pair = IntLngPair.of((int) 0, (long) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(IntLngPredicate pred0, IntLngSource source2) {
		IntLngPredicate pred1 = pred0.rethrow();
		IntLngPair pair = IntLngPair.of((int) 0, (long) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<IntLngPair> iterator(IntLngSource source2) {
		return new Iterator<IntLngPair>() {
			private IntLngPair next = null;

			public boolean hasNext() {
				if (next == null) {
					IntLngPair next1 = IntLngPair.of((int) 0, (long) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public IntLngPair next() {
				IntLngPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<IntLngPair> iter(IntLngSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(IntLng_Obj<T> fun0, IntLngSource source2) {
		IntLng_Obj<T> fun1 = fun0.rethrow();
		IntLngPair pair = IntLngPair.of((int) 0, (long) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(IntLng_Obj<K1> kf0, IntLng_Obj<V1> vf0, IntLngSource source2) {
		IntLng_Obj<K1> kf1 = kf0.rethrow();
		IntLng_Obj<V1> vf1 = vf0.rethrow();
		IntLngPair pair1 = IntLngPair.of((int) 0, (long) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static IntLngSource mapIntLng(IntLng_Int kf0, IntLng_Lng vf0, IntLngSource source2) {
		IntLng_Int kf1 = kf0.rethrow();
		IntLng_Lng vf1 = vf0.rethrow();
		IntLngPair pair1 = IntLngPair.of((int) 0, (long) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(IntLng_Obj<T> fun0, IntLngSource source) {
		IntLng_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				IntLngPair pair = IntLngPair.of((int) 0, (long) 0);
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

	public static <V> IntLngSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<IntLngSource> split(IntLngPredicate fun0, IntLngSource source2) {
		IntLngPredicate fun1 = fun0.rethrow();
		return new Source<IntLngSource>() {
			private IntLngPair pair = IntLngPair.of((int) 0, (long) 0);
			private boolean isAvailable;
			private IntLngSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public IntLngSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> IntLngSource suck(Sink<Sink<IntLngPair>> fun) {
		NullableSyncQueue<IntLngPair> queue = new NullableSyncQueue<>();
		Sink<IntLngPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				IntLngPair p = queue.take();
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
