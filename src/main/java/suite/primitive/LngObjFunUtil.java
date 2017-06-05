package suite.primitive;

import java.util.Iterator;
import java.util.function.Predicate;

import suite.adt.pair.LngObjPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.LngFun.LngObj_Obj;
import suite.primitive.LngPredicate.LngObjPredicate;
import suite.primitive.LngPredicate.LngPredicate_;
import suite.primitive.LngSource.LngObjSource;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class LngObjFunUtil {

	public static <V> LngObjSource<V> append(long key, V value, LngObjSource<V> source) {
		return new LngObjSource<V>() {
			private boolean isAppended = false;

			public boolean source2(LngObjPair<V> pair) {
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

	public static <V> Source<LngObjSource<V>> chunk(int n, LngObjSource<V> source) {
		return new Source<LngObjSource<V>>() {
			private LngObjPair<V> pair;
			private boolean isAvail;
			private int i;
			private LngObjSource<V> source_ = pair1 -> {
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

			public LngObjSource<V> source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> LngObjSource<V> concat(Source<LngObjSource<V>> source) {
		return new LngObjSource<V>() {
			private LngObjSource<V> source2 = nullSource();

			public boolean source2(LngObjPair<V> pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static <V> LngObjSource<V> cons(long key, V value, LngObjSource<V> source2) {
		return new LngObjSource<V>() {
			private boolean isFirst = true;

			public boolean source2(LngObjPair<V> pair) {
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

	public static <V> LngObjSource<V> filter(LngObjPredicate<V> fun0, LngObjSource<V> source2) {
		LngObjPredicate<V> fun1 = LngRethrow.lngObjPredicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> LngObjSource<V> filterKey(LngPredicate_ fun0, LngObjSource<V> source2) {
		LngPredicate_ fun1 = LngRethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static <V> LngObjSource<V> filterValue(Predicate<V> fun0, LngObjSource<V> source2) {
		Predicate<V> fun1 = Rethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, LngObjPair<V>>, R> fun0, R init, LngObjSource<V> source2) {
		Fun<Pair<R, LngObjPair<V>>, R> fun1 = Rethrow.fun(fun0);
		LngObjPair<V> pair = LngObjPair.of((long) 0, null);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static <V> boolean isAll(LngObjPredicate<V> pred0, LngObjSource<V> source2) {
		LngObjPredicate<V> pred1 = LngRethrow.lngObjPredicate(pred0);
		LngObjPair<V> pair = LngObjPair.of((long) 0, null);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(LngObjPredicate<V> pred0, LngObjSource<V> source2) {
		LngObjPredicate<V> pred1 = LngRethrow.lngObjPredicate(pred0);
		LngObjPair<V> pair = LngObjPair.of((long) 0, null);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<LngObjPair<V>> iterator(LngObjSource<V> source2) {
		return new Iterator<LngObjPair<V>>() {
			private LngObjPair<V> next = null;

			public boolean hasNext() {
				if (next == null) {
					LngObjPair<V> next1 = LngObjPair.of((long) 0, null);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public LngObjPair<V> next() {
				LngObjPair<V> next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<LngObjPair<V>> iter(LngObjSource<V> source2) {
		return () -> iterator(source2);
	}

	public static <V, T> Source<T> map(LngObj_Obj<V, T> fun0, LngObjSource<V> source2) {
		LngObj_Obj<V, T> fun1 = LngRethrow.fun2(fun0);
		LngObjPair<V> pair = LngObjPair.of((long) 0, null);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <V, K1, V1, T> Source2<K1, V1> map2(LngObj_Obj<V, K1> kf0, LngObj_Obj<V, V1> vf0, LngObjSource<V> source2) {
		LngObj_Obj<V, K1> kf1 = LngRethrow.fun2(kf0);
		LngObj_Obj<V, V1> vf1 = LngRethrow.fun2(vf0);
		LngObjPair<V> pair1 = LngObjPair.of((long) 0, null);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <V, V1, T> LngObjSource<V1> mapLngObj(LngObj_Lng<V> kf0, LngObj_Obj<V, V1> vf0, LngObjSource<V> source2) {
		LngObj_Lng<V> kf1 = LngLngRethrow.fun2(kf0);
		LngObj_Obj<V, V1> vf1 = LngRethrow.fun2(vf0);
		LngObjPair<V> pair1 = LngObjPair.of((long) 0, null);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <V, T1> Source<T1> mapNonNull(LngObj_Obj<V, T1> fun, LngObjSource<V> source) {
		return new Source<T1>() {
			public T1 source() {
				LngObjPair<V> pair = LngObjPair.of((long) 0, null);
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

	public static <V> LngObjSource<V> nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static <V> Source<LngObjSource<V>> split(LngObjPredicate<V> fun0, LngObjSource<V> source2) {
		LngObjPredicate<V> fun1 = LngRethrow.lngObjPredicate(fun0);
		return new Source<LngObjSource<V>>() {
			private LngObjPair<V> pair = LngObjPair.of((long) 0, null);
			private boolean isAvailable;
			private LngObjSource<V> source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public LngObjSource<V> source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> LngObjSource<V> suck(Sink<Sink<LngObjPair<V>>> fun) {
		NullableSyncQueue<LngObjPair<V>> queue = new NullableSyncQueue<>();
		Sink<LngObjPair<V>> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				LngObjPair<V> p = queue.take();
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
