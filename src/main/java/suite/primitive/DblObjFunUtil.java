package suite.primitive;

import java.util.Iterator;
import java.util.function.Predicate;

import suite.adt.pair.DblObjPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.DblFun.DblObj_Obj;
import suite.primitive.DblPredicate.DblObjPredicate;
import suite.primitive.DblPredicate.DblPredicate_;
import suite.primitive.DblSource.DblObjSource;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class DblObjFunUtil {

	public static <V> DblObjSource<V> append(double key, V value, DblObjSource<V> source) {
		return new DblObjSource<V>() {
			private boolean isAppended = false;

			public boolean source2(DblObjPair<V> pair) {
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

	public static <V> Source<DblObjSource<V>> chunk(int n, DblObjSource<V> source) {
		return new Source<DblObjSource<V>>() {
			private DblObjPair<V> pair;
			private boolean isAvail;
			private int i;
			private DblObjSource<V> source_ = pair1 -> {
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

			public DblObjSource<V> source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> DblObjSource<V> concat(Source<DblObjSource<V>> source) {
		return new DblObjSource<V>() {
			private DblObjSource<V> source2 = nullSource();

			public boolean source2(DblObjPair<V> pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static <V> DblObjSource<V> cons(double key, V value, DblObjSource<V> source2) {
		return new DblObjSource<V>() {
			private boolean isFirst = true;

			public boolean source2(DblObjPair<V> pair) {
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

	public static <V> DblObjSource<V> filter(DblObjPredicate<V> fun0, DblObjSource<V> source2) {
		DblObjPredicate<V> fun1 = DblRethrow.dblObjPredicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> DblObjSource<V> filterKey(DblPredicate_ fun0, DblObjSource<V> source2) {
		DblPredicate_ fun1 = DblRethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static <V> DblObjSource<V> filterValue(Predicate<V> fun0, DblObjSource<V> source2) {
		Predicate<V> fun1 = Rethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, DblObjPair<V>>, R> fun0, R init, DblObjSource<V> source2) {
		Fun<Pair<R, DblObjPair<V>>, R> fun1 = Rethrow.fun(fun0);
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static <V> boolean isAll(DblObjPredicate<V> pred0, DblObjSource<V> source2) {
		DblObjPredicate<V> pred1 = DblRethrow.dblObjPredicate(pred0);
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(DblObjPredicate<V> pred0, DblObjSource<V> source2) {
		DblObjPredicate<V> pred1 = DblRethrow.dblObjPredicate(pred0);
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<DblObjPair<V>> iterator(DblObjSource<V> source2) {
		return new Iterator<DblObjPair<V>>() {
			private DblObjPair<V> next = null;

			public boolean hasNext() {
				if (next == null) {
					DblObjPair<V> next1 = DblObjPair.of((double) 0, null);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public DblObjPair<V> next() {
				DblObjPair<V> next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<DblObjPair<V>> iter(DblObjSource<V> source2) {
		return () -> iterator(source2);
	}

	public static <V, T> Source<T> map(DblObj_Obj<V, T> fun0, DblObjSource<V> source2) {
		DblObj_Obj<V, T> fun1 = DblRethrow.fun2(fun0);
		DblObjPair<V> pair = DblObjPair.of((double) 0, null);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <V, K1, V1, T> Source2<K1, V1> map2(DblObj_Obj<V, K1> kf0, DblObj_Obj<V, V1> vf0, DblObjSource<V> source2) {
		DblObj_Obj<V, K1> kf1 = DblRethrow.fun2(kf0);
		DblObj_Obj<V, V1> vf1 = DblRethrow.fun2(vf0);
		DblObjPair<V> pair1 = DblObjPair.of((double) 0, null);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <V, V1, T> DblObjSource<V1> mapDblObj(DblObj_Dbl<V> kf0, DblObj_Obj<V, V1> vf0, DblObjSource<V> source2) {
		DblObj_Dbl<V> kf1 = DblDblRethrow.fun2(kf0);
		DblObj_Obj<V, V1> vf1 = DblRethrow.fun2(vf0);
		DblObjPair<V> pair1 = DblObjPair.of((double) 0, null);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <V, T1> Source<T1> mapNonNull(DblObj_Obj<V, T1> fun, DblObjSource<V> source) {
		return new Source<T1>() {
			public T1 source() {
				DblObjPair<V> pair = DblObjPair.of((double) 0, null);
				T1 t1 = null;
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

	public static <V> DblObjSource<V> nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static <V> Source<DblObjSource<V>> split(DblObjPredicate<V> fun0, DblObjSource<V> source2) {
		DblObjPredicate<V> fun1 = DblRethrow.dblObjPredicate(fun0);
		return new Source<DblObjSource<V>>() {
			private DblObjPair<V> pair = DblObjPair.of((double) 0, null);
			private boolean isAvailable;
			private DblObjSource<V> source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public DblObjSource<V> source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> DblObjSource<V> suck(Sink<Sink<DblObjPair<V>>> fun) {
		NullableSyncQueue<DblObjPair<V>> queue = new NullableSyncQueue<>();
		Sink<DblObjPair<V>> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				DblObjPair<V> p = queue.take();
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
