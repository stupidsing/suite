package suite.primitive;

import java.util.Iterator;
import java.util.function.Predicate;

import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.FltPrimitives.FltObjPredicate;
import suite.primitive.FltPrimitives.FltObjSource;
import suite.primitive.FltPrimitives.FltObj_Obj;
import suite.primitive.FltPrimitives.FltTest;
import suite.primitive.adt.pair.FltObjPair;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class FltObjFunUtil {

	public static <V> FltObjSource<V> append(float key, V value, FltObjSource<V> source) {
		return new FltObjSource<>() {
			private boolean isAppended = false;

			public boolean source2(FltObjPair<V> pair) {
				if (!isAppended) {
					if (!source.source2(pair)) {
						pair.update(key, value);
						isAppended = true;
					}
					return true;
				} else
					return false;
			}
		};
	}

	public static <V> Source<FltObjSource<V>> chunk(int n, FltObjSource<V> source) {
		return new Source<>() {
			private FltObjPair<V> pair;
			private boolean isAvail;
			private int i;
			private FltObjSource<V> source_ = pair1 -> {
				boolean b = (isAvail = isAvail && source.source2(pair)) && ++i < n;
				if (b)
					pair1.update(pair.t0, pair.t1);
				else
					i = 0;
				return b;
			};

			{
				isAvail = source.source2(pair);
			}

			public FltObjSource<V> source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> FltObjSource<V> concat(Source<FltObjSource<V>> source) {
		return new FltObjSource<>() {
			private FltObjSource<V> source2 = nullSource();

			public boolean source2(FltObjPair<V> pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static <V> FltObjSource<V> cons(float key, V value, FltObjSource<V> source2) {
		return new FltObjSource<>() {
			private boolean isFirst = true;

			public boolean source2(FltObjPair<V> pair) {
				if (!isFirst)
					return source2.source2(pair);
				else {
					isFirst = false;
					pair.update(key, value);
					return true;
				}
			}
		};
	}

	public static <V> FltObjSource<V> filter(FltObjPredicate<V> fun0, FltObjSource<V> source2) {
		FltObjPredicate<V> fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> FltObjSource<V> filterKey(FltTest fun0, FltObjSource<V> source2) {
		FltTest fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static <V> FltObjSource<V> filterValue(Predicate<V> fun0, FltObjSource<V> source2) {
		Predicate<V> fun1 = Rethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, FltObjPair<V>>, R> fun0, R init, FltObjSource<V> source2) {
		Fun<Pair<R, FltObjPair<V>>, R> fun1 = fun0.rethrow();
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static <V> boolean isAll(FltObjPredicate<V> pred0, FltObjSource<V> source2) {
		FltObjPredicate<V> pred1 = pred0.rethrow();
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(FltObjPredicate<V> pred0, FltObjSource<V> source2) {
		FltObjPredicate<V> pred1 = pred0.rethrow();
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<FltObjPair<V>> iterator(FltObjSource<V> source2) {
		return new Iterator<>() {
			private FltObjPair<V> next = null;

			public boolean hasNext() {
				if (next == null) {
					FltObjPair<V> next1 = FltObjPair.of((float) 0, null);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public FltObjPair<V> next() {
				FltObjPair<V> next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<FltObjPair<V>> iter(FltObjSource<V> source2) {
		return () -> iterator(source2);
	}

	public static <V, T> Source<T> map(FltObj_Obj<V, T> fun0, FltObjSource<V> source2) {
		FltObj_Obj<V, T> fun1 = fun0.rethrow();
		FltObjPair<V> pair = FltObjPair.of((float) 0, null);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <V, K1, V1, T> Source2<K1, V1> map2(FltObj_Obj<V, K1> kf0, FltObj_Obj<V, V1> vf0, FltObjSource<V> source2) {
		FltObj_Obj<V, K1> kf1 = kf0.rethrow();
		FltObj_Obj<V, V1> vf1 = vf0.rethrow();
		FltObjPair<V> pair1 = FltObjPair.of((float) 0, null);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b)
				pair.update(kf1.apply(pair1.t0, pair1.t1), vf1.apply(pair1.t0, pair1.t1));
			return b;
		};
	}

	public static <V, V1, T> FltObjSource<V1> mapFltObj(FltObj_Flt<V> kf0, FltObj_Obj<V, V1> vf0, FltObjSource<V> source2) {
		FltObj_Flt<V> kf1 = kf0.rethrow();
		FltObj_Obj<V, V1> vf1 = vf0.rethrow();
		FltObjPair<V> pair1 = FltObjPair.of((float) 0, null);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b)
				pair.update(kf1.apply(pair1.t0, pair1.t1), vf1.apply(pair1.t0, pair1.t1));
			return b;
		};
	}

	public static <I> Sink<I> nullSink() {
		return i -> {
		};
	}

	public static <V> FltObjSource<V> nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static <V> Source<FltObjSource<V>> split(FltObjPredicate<V> fun0, FltObjSource<V> source2) {
		FltObjPredicate<V> fun1 = fun0.rethrow();
		return new Source<>() {
			private FltObjPair<V> pair = FltObjPair.of((float) 0, null);
			private boolean isAvailable;
			private FltObjSource<V> source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public FltObjSource<V> source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> FltObjSource<V> suck(Sink<Sink<FltObjPair<V>>> fun) {
		NullableSyncQueue<FltObjPair<V>> queue = new NullableSyncQueue<>();
		Sink<FltObjPair<V>> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				FltObjPair<V> p = queue.take();
				boolean b = p != null;
				if (b)
					pair.update(p.t0, p.t1);
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
