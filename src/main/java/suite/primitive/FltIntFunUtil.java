package suite.primitive;

import java.util.Iterator;

import suite.adt.pair.FltIntPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.FltPrimitives.FltPredicate_;
import suite.primitive.IntPrimitives.IntPredicate_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class FltIntFunUtil {

	public static FltIntSource append(float key, int value, FltIntSource source) {
		return new FltIntSource() {
			private boolean isAppended = false;

			public boolean source2(FltIntPair pair) {
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

	public static <V> Source<FltIntSource> chunk(int n, FltIntSource source) {
		return new Source<FltIntSource>() {
			private FltIntPair pair;
			private boolean isAvail;
			private int i;
			private FltIntSource source_ = pair1 -> {
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

			public FltIntSource source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> FltIntSource concat(Source<FltIntSource> source) {
		return new FltIntSource() {
			private FltIntSource source2 = nullSource();

			public boolean source2(FltIntPair pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static FltIntSource cons(float key, int value, FltIntSource source2) {
		return new FltIntSource() {
			private boolean isFirst = true;

			public boolean source2(FltIntPair pair) {
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

	public static FltIntSource filter(FltIntPredicate fun0, FltIntSource source2) {
		FltIntPredicate fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> FltIntSource filterKey(FltPredicate_ fun0, FltIntSource source2) {
		FltPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static FltIntSource filterValue(IntPredicate_ fun0, FltIntSource source2) {
		IntPredicate_ fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, FltIntPair>, R> fun0, R init, FltIntSource source2) {
		Fun<Pair<R, FltIntPair>, R> fun1 = Rethrow.fun(fun0);
		FltIntPair pair = FltIntPair.of((float) 0, (int) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(FltIntPredicate pred0, FltIntSource source2) {
		FltIntPredicate pred1 = pred0.rethrow();
		FltIntPair pair = FltIntPair.of((float) 0, (int) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(FltIntPredicate pred0, FltIntSource source2) {
		FltIntPredicate pred1 = pred0.rethrow();
		FltIntPair pair = FltIntPair.of((float) 0, (int) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<FltIntPair> iterator(FltIntSource source2) {
		return new Iterator<FltIntPair>() {
			private FltIntPair next = null;

			public boolean hasNext() {
				if (next == null) {
					FltIntPair next1 = FltIntPair.of((float) 0, (int) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public FltIntPair next() {
				FltIntPair next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<FltIntPair> iter(FltIntSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(FltInt_Obj<T> fun0, FltIntSource source2) {
		FltInt_Obj<T> fun1 = fun0.rethrow();
		FltIntPair pair = FltIntPair.of((float) 0, (int) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(FltInt_Obj<K1> kf0, FltInt_Obj<V1> vf0, FltIntSource source2) {
		FltInt_Obj<K1> kf1 = kf0.rethrow();
		FltInt_Obj<V1> vf1 = vf0.rethrow();
		FltIntPair pair1 = FltIntPair.of((float) 0, (int) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static FltIntSource mapFltInt(FltInt_Flt kf0, FltInt_Int vf0, FltIntSource source2) {
		FltInt_Flt kf1 = kf0.rethrow();
		FltInt_Int vf1 = vf0.rethrow();
		FltIntPair pair1 = FltIntPair.of((float) 0, (int) 0);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <T> Source<T> mapNonNull(FltInt_Obj<T> fun0, FltIntSource source) {
		FltInt_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				FltIntPair pair = FltIntPair.of((float) 0, (int) 0);
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

	public static <V> FltIntSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<FltIntSource> split(FltIntPredicate fun0, FltIntSource source2) {
		FltIntPredicate fun1 = fun0.rethrow();
		return new Source<FltIntSource>() {
			private FltIntPair pair = FltIntPair.of((float) 0, (int) 0);
			private boolean isAvailable;
			private FltIntSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public FltIntSource source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> FltIntSource suck(Sink<Sink<FltIntPair>> fun) {
		NullableSyncQueue<FltIntPair> queue = new NullableSyncQueue<>();
		Sink<FltIntPair> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				FltIntPair p = queue.take();
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
