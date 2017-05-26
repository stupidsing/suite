package suite.primitive;

import java.util.Iterator;
import java.util.function.Predicate;

import suite.adt.pair.ChrObjPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.ChrPrimitiveFun.ChrObj_Obj;
import suite.primitive.ChrPrimitivePredicate.ChrObjPredicate;
import suite.primitive.ChrPrimitivePredicate.ChrPredicate_;
import suite.primitive.ChrPrimitiveSource.ChrObjSource;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class ChrObjFunUtil {

	public static <V> ChrObjSource<V> append(char key, V value, ChrObjSource<V> source) {
		return new ChrObjSource<V>() {
			private boolean isAppended = false;

			public boolean source2(ChrObjPair<V> pair) {
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

	public static <V> Source<ChrObjSource<V>> chunk(int n, ChrObjSource<V> source) {
		return new Source<ChrObjSource<V>>() {
			private ChrObjPair<V> pair;
			private boolean isAvail;
			private int i;
			private ChrObjSource<V> source_ = pair1 -> {
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

			public ChrObjSource<V> source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> ChrObjSource<V> concat(Source<ChrObjSource<V>> source) {
		return new ChrObjSource<V>() {
			private ChrObjSource<V> source2 = nullSource();

			public boolean source2(ChrObjPair<V> pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static <V> ChrObjSource<V> cons(char key, V value, ChrObjSource<V> source2) {
		return new ChrObjSource<V>() {
			private boolean isFirst = true;

			public boolean source2(ChrObjPair<V> pair) {
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

	public static <V> ChrObjSource<V> filter(ChrObjPredicate<V> fun0, ChrObjSource<V> source2) {
		ChrObjPredicate<V> fun1 = ChrRethrow.chrObjPredicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> ChrObjSource<V> filterKey(ChrPredicate_ fun0, ChrObjSource<V> source2) {
		ChrPredicate_ fun1 = ChrRethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static <V> ChrObjSource<V> filterValue(Predicate<V> fun0, ChrObjSource<V> source2) {
		Predicate<V> fun1 = Rethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, ChrObjPair<V>>, R> fun0, R init, ChrObjSource<V> source2) {
		Fun<Pair<R, ChrObjPair<V>>, R> fun1 = Rethrow.fun(fun0);
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static <V> boolean isAll(ChrObjPredicate<V> pred0, ChrObjSource<V> source2) {
		ChrObjPredicate<V> pred1 = ChrRethrow.chrObjPredicate(pred0);
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(ChrObjPredicate<V> pred0, ChrObjSource<V> source2) {
		ChrObjPredicate<V> pred1 = ChrRethrow.chrObjPredicate(pred0);
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<ChrObjPair<V>> iterator(ChrObjSource<V> source2) {
		return new Iterator<ChrObjPair<V>>() {
			private ChrObjPair<V> next = null;

			public boolean hasNext() {
				if (next == null) {
					ChrObjPair<V> next1 = ChrObjPair.of((char) 0, null);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public ChrObjPair<V> next() {
				ChrObjPair<V> next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<ChrObjPair<V>> iter(ChrObjSource<V> source2) {
		return () -> iterator(source2);
	}

	public static <V, T> Source<T> map(ChrObj_Obj<V, T> fun0, ChrObjSource<V> source2) {
		ChrObj_Obj<V, T> fun1 = ChrRethrow.fun2(fun0);
		ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <V, K1, V1, T> Source2<K1, V1> map2(ChrObj_Obj<V, K1> kf0, ChrObj_Obj<V, V1> vf0, ChrObjSource<V> source2) {
		ChrObj_Obj<V, K1> kf1 = ChrRethrow.fun2(kf0);
		ChrObj_Obj<V, V1> vf1 = ChrRethrow.fun2(vf0);
		ChrObjPair<V> pair1 = ChrObjPair.of((char) 0, null);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <V, V1, T> ChrObjSource<V1> mapChrObj(ChrObj_Chr<V> kf0, ChrObj_Obj<V, V1> vf0, ChrObjSource<V> source2) {
		ChrObj_Chr<V> kf1 = ChrChrRethrow.fun2(kf0);
		ChrObj_Obj<V, V1> vf1 = ChrRethrow.fun2(vf0);
		ChrObjPair<V> pair1 = ChrObjPair.of((char) 0, null);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <V, T1> Source<T1> mapNonNull(ChrObj_Obj<V, T1> fun, ChrObjSource<V> source) {
		return new Source<T1>() {
			public T1 source() {
				ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
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

	public static <V> ChrObjSource<V> nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static <V> Source<ChrObjSource<V>> split(ChrObjPredicate<V> fun0, ChrObjSource<V> source2) {
		ChrObjPredicate<V> fun1 = ChrRethrow.chrObjPredicate(fun0);
		return new Source<ChrObjSource<V>>() {
			private ChrObjPair<V> pair = ChrObjPair.of((char) 0, null);
			private boolean isAvailable;
			private ChrObjSource<V> source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public ChrObjSource<V> source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> ChrObjSource<V> suck(Sink<Sink<ChrObjPair<V>>> fun) {
		NullableSyncQueue<ChrObjPair<V>> queue = new NullableSyncQueue<>();
		Sink<ChrObjPair<V>> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				ChrObjPair<V> p = queue.take();
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
