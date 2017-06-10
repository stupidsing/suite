package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.DblIntPair;
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

public class DblIntFunUtil {

	public static DblIntSource append(double key, int value, DblIntSource source) {
		return new DblIntSource() {
			private boolean isAppended = false;

			public boolean source2(DblIntPair pair) {
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

	public static <V> Source<DblIntSource> chunk(int n, DblIntSource source) {
		return new Source<DblIntSource>() {
			private DblIntPair pair;
			private boolean isAvail;
			private int i;
			private DblIntSource source_ = pair1 -> {
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

			public DblIntSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> DblIntSource concat(Source<DblIntSource> source) {
		return new DblIntSource() {
			private DblIntSource source2 = nullSource();

			public boolean source2(DblIntPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static DblIntSource cons(double key, int value, DblIntSource source2) {
		return new DblIntSource() {
			private boolean isFirst = true;

			public boolean source2(DblIntPair pair) {
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

	public static DblIntSource filter(DblIntPredicate fun0, DblIntSource source2) {
		DblIntPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> DblIntSource filterKey(DblPredicate fun0, DblIntSource source2) {
		DblPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static DblIntSource filterValue(IntPredicate fun0, DblIntSource source2) {
		IntPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, DblIntPair>, R> fun0, R init, DblIntSource source2) {
		Fun<Pair<R, DblIntPair>, R> fun1 = Rethrow.fun(fun0);
		DblIntPair pair = DblIntPair.of((double) 0, (int) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(DblIntPredicate pred0, DblIntSource source2) {
		DblIntPredicate pred1 = pred0.rethrow();
		DblIntPair pair = DblIntPair.of((double) 0, (int) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(DblIntPredicate pred0, DblIntSource source2) {
		DblIntPredicate pred1 = pred0.rethrow();
		DblIntPair pair = DblIntPair.of((double) 0, (int) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<DblIntPair> iterator(DblIntSource source2) {
		return new Iterator<DblIntPair>() {
			private DblIntPair next = null;

			public boolean hasNext() {
				if (next == null) {
					DblIntPair next1 = DblIntPair.of((double) 0, (int) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public DblIntPair next() {
				DblIntPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<DblIntPair> iter(DblIntSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(DblInt_Obj<T> fun0, DblIntSource source2) {
		DblInt_Obj<T> fun1 = fun0.rethrow();
		DblIntPair pair = DblIntPair.of((double) 0, (int) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(DblInt_Obj<K1> kf0, DblInt_Obj<V1> vf0, DblIntSource source2) {
		DblInt_Obj<K1> kf1 = kf0.rethrow();
		DblInt_Obj<V1> vf1 = vf0.rethrow();
		DblIntPair pair1 = DblIntPair.of((double) 0, (int) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static DblIntSource mapDblInt(DblInt_Dbl kf0, DblInt_Int vf0, DblIntSource source2) {
		DblInt_Dbl kf1 = kf0.rethrow();
		DblInt_Int vf1 = vf0.rethrow();
		DblIntPair pair1 = DblIntPair.of((double) 0, (int) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(DblInt_Obj<T> fun0, DblIntSource source) {
		DblInt_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				DblIntPair pair = DblIntPair.of((double) 0, (int) 0);
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

	public static <V> DblIntSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<DblIntSource> split(DblIntPredicate fun0, DblIntSource source2) {
		DblIntPredicate fun1 = fun0.rethrow();
		return new Source<DblIntSource>() {
			private DblIntPair pair = DblIntPair.of((double) 0, (int) 0);
			private boolean isAvailable;
			private DblIntSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public DblIntSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> DblIntSource suck(Sink<Sink<DblIntPair>> fun) {
		NullableSyncQueue<DblIntPair> queue = new NullableSyncQueue<>();
		Sink<DblIntPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				DblIntPair p = queue.take();
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
