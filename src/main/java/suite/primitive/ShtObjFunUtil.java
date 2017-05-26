package suite.primitive;

import java.util.Iterator;
import java.util.function.Predicate;

import suite.adt.pair.ShtObjPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.ShtPrimitiveFun.ShtObj_Obj;
import suite.primitive.ShtPrimitivePredicate.ShtObjPredicate;
import suite.primitive.ShtPrimitivePredicate.ShtPredicate_;
import suite.primitive.ShtPrimitiveSource.ShtObjSource;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class ShtObjFunUtil {

	public static <V> ShtObjSource<V> append(short key, V value, ShtObjSource<V> source) {
		return new ShtObjSource<V>() {
			private boolean isAppended = false;

			public boolean source2(ShtObjPair<V> pair) {
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

	public static <V> Source<ShtObjSource<V>> chunk(int n, ShtObjSource<V> source) {
		return new Source<ShtObjSource<V>>() {
			private ShtObjPair<V> pair;
			private boolean isAvail;
			private int i;
			private ShtObjSource<V> source_ = pair1 -> {
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

			public ShtObjSource<V> source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> ShtObjSource<V> concat(Source<ShtObjSource<V>> source) {
		return new ShtObjSource<V>() {
			private ShtObjSource<V> source2 = nullSource();

			public boolean source2(ShtObjPair<V> pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static <V> ShtObjSource<V> cons(short key, V value, ShtObjSource<V> source2) {
		return new ShtObjSource<V>() {
			private boolean isFirst = true;

			public boolean source2(ShtObjPair<V> pair) {
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

	public static <V> ShtObjSource<V> filter(ShtObjPredicate<V> fun0, ShtObjSource<V> source2) {
		ShtObjPredicate<V> fun1 = ShtRethrow.shtObjPredicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> ShtObjSource<V> filterKey(ShtPredicate_ fun0, ShtObjSource<V> source2) {
		ShtPredicate_ fun1 = ShtRethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static <V> ShtObjSource<V> filterValue(Predicate<V> fun0, ShtObjSource<V> source2) {
		Predicate<V> fun1 = Rethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, ShtObjPair<V>>, R> fun0, R init, ShtObjSource<V> source2) {
		Fun<Pair<R, ShtObjPair<V>>, R> fun1 = Rethrow.fun(fun0);
		ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static <V> boolean isAll(ShtObjPredicate<V> pred0, ShtObjSource<V> source2) {
		ShtObjPredicate<V> pred1 = ShtRethrow.shtObjPredicate(pred0);
		ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(ShtObjPredicate<V> pred0, ShtObjSource<V> source2) {
		ShtObjPredicate<V> pred1 = ShtRethrow.shtObjPredicate(pred0);
		ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<ShtObjPair<V>> iterator(ShtObjSource<V> source2) {
		return new Iterator<ShtObjPair<V>>() {
			private ShtObjPair<V> next = null;

			public boolean hasNext() {
				if (next == null) {
					ShtObjPair<V> next1 = ShtObjPair.of((short) 0, null);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public ShtObjPair<V> next() {
				ShtObjPair<V> next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<ShtObjPair<V>> iter(ShtObjSource<V> source2) {
		return () -> iterator(source2);
	}

	public static <V, T> Source<T> map(ShtObj_Obj<V, T> fun0, ShtObjSource<V> source2) {
		ShtObj_Obj<V, T> fun1 = ShtRethrow.fun2(fun0);
		ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <V, K1, V1, T> Source2<K1, V1> map2(ShtObj_Obj<V, K1> kf0, ShtObj_Obj<V, V1> vf0, ShtObjSource<V> source2) {
		ShtObj_Obj<V, K1> kf1 = ShtRethrow.fun2(kf0);
		ShtObj_Obj<V, V1> vf1 = ShtRethrow.fun2(vf0);
		ShtObjPair<V> pair1 = ShtObjPair.of((short) 0, null);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <V, V1, T> ShtObjSource<V1> mapShtObj(ShtObj_Sht<V> kf0, ShtObj_Obj<V, V1> vf0, ShtObjSource<V> source2) {
		ShtObj_Sht<V> kf1 = ShtShtRethrow.fun2(kf0);
		ShtObj_Obj<V, V1> vf1 = ShtRethrow.fun2(vf0);
		ShtObjPair<V> pair1 = ShtObjPair.of((short) 0, null);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <V, T1> Source<T1> mapNonNull(ShtObj_Obj<V, T1> fun, ShtObjSource<V> source) {
		return new Source<T1>() {
			public T1 source() {
				ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
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

	public static <V> ShtObjSource<V> nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static <V> Source<ShtObjSource<V>> split(ShtObjPredicate<V> fun0, ShtObjSource<V> source2) {
		ShtObjPredicate<V> fun1 = ShtRethrow.shtObjPredicate(fun0);
		return new Source<ShtObjSource<V>>() {
			private ShtObjPair<V> pair = ShtObjPair.of((short) 0, null);
			private boolean isAvailable;
			private ShtObjSource<V> source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public ShtObjSource<V> source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> ShtObjSource<V> suck(Sink<Sink<ShtObjPair<V>>> fun) {
		NullableSyncQueue<ShtObjPair<V>> queue = new NullableSyncQueue<>();
		Sink<ShtObjPair<V>> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				ShtObjPair<V> p = queue.take();
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
