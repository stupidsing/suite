package suite.primitive;

import java.util.Iterator;
import java.util.function.Predicate;

import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.IntPrimitives.IntObjPredicate;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.IntObj_Obj;
import suite.primitive.IntPrimitives.IntTest;
import suite.primitive.adt.pair.IntObjPair;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class IntObjFunUtil {

	public static <V> Source<IntObjSource<V>> chunk(int n, IntObjSource<V> source) {
		return new Source<>() {
			private IntObjPair<V> pair;
			private boolean isAvail;
			private int i;
			private IntObjSource<V> source_ = pair1 -> {
				var b = (isAvail = isAvail && source.source2(pair)) && ++i < n;
				if (b)
					pair1.update(pair.t0, pair.t1);
				else
					i = 0;
				return b;
			};

			{
				isAvail = source.source2(pair);
			}

			public IntObjSource<V> source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> IntObjSource<V> concat(Source<IntObjSource<V>> source) {
		return new IntObjSource<>() {
			private IntObjSource<V> source2 = nullSource();

			public boolean source2(IntObjPair<V> pair) {
				var b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static <V> IntObjSource<V> cons(int key, V value, IntObjSource<V> source2) {
		return new IntObjSource<>() {
			private boolean isFirst = true;

			public boolean source2(IntObjPair<V> pair) {
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

	public static <V> IntObjSource<V> filter(IntObjPredicate<V> fun0, IntObjSource<V> source2) {
		var fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> IntObjSource<V> filterKey(IntTest fun0, IntObjSource<V> source2) {
		var fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static <V> IntObjSource<V> filterValue(Predicate<V> fun0, IntObjSource<V> source2) {
		var fun1 = Rethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, IntObjPair<V>>, R> fun0, R init, IntObjSource<V> source2) {
		var fun1 = fun0.rethrow();
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static <V> boolean isAll(IntObjPredicate<V> pred0, IntObjSource<V> source2) {
		var pred1 = pred0.rethrow();
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(IntObjPredicate<V> pred0, IntObjSource<V> source2) {
		var pred1 = pred0.rethrow();
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<IntObjPair<V>> iterator(IntObjSource<V> source2) {
		return new Iterator<>() {
			private IntObjPair<V> next = null;

			public boolean hasNext() {
				if (next == null) {
					IntObjPair<V> next1 = IntObjPair.of((int) 0, null);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public IntObjPair<V> next() {
				var next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<IntObjPair<V>> iter(IntObjSource<V> source2) {
		return () -> iterator(source2);
	}

	public static <V, T> Source<T> map(IntObj_Obj<V, T> fun0, IntObjSource<V> source2) {
		var fun1 = fun0.rethrow();
		IntObjPair<V> pair = IntObjPair.of((int) 0, null);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <V, K1, V1, T> Source2<K1, V1> map2(IntObj_Obj<V, K1> kf0, IntObj_Obj<V, V1> vf0, IntObjSource<V> source2) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		IntObjPair<V> pair1 = IntObjPair.of((int) 0, null);
		return pair -> {
			var b = source2.source2(pair1);
			if (b)
				pair.update(kf1.apply(pair1.t0, pair1.t1), vf1.apply(pair1.t0, pair1.t1));
			return b;
		};
	}

	public static <V, V1, T> IntObjSource<V1> mapIntObj(IntObj_Int<V> kf0, IntObj_Obj<V, V1> vf0, IntObjSource<V> source2) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		IntObjPair<V> pair1 = IntObjPair.of((int) 0, null);
		return pair -> {
			var b = source2.source2(pair1);
			if (b)
				pair.update(kf1.apply(pair1.t0, pair1.t1), vf1.apply(pair1.t0, pair1.t1));
			return b;
		};
	}

	public static <I> Sink<I> nullSink() {
		return i -> {
		};
	}

	public static <V> IntObjSource<V> nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must not be
	 * skipped.
	 */
	public static <V> Source<IntObjSource<V>> split(IntObjPredicate<V> fun0, IntObjSource<V> source2) {
		IntObjPredicate<V> fun1 = fun0.rethrow();
		return new Source<>() {
			private IntObjPair<V> pair = IntObjPair.of((int) 0, null);
			private boolean isAvailable;
			private IntObjSource<V> source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public IntObjSource<V> source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	public static <V> IntObjSource<V> snoc(int key, V value, IntObjSource<V> source) {
		return new IntObjSource<>() {
			private boolean isAppended = false;

			public boolean source2(IntObjPair<V> pair) {
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

	/**
	 * Sucks data from a sink and make it into a source.
	 */
	public static <V> IntObjSource<V> suck(Sink<Sink<IntObjPair<V>>> fun) {
		var queue = new NullableSyncQueue<IntObjPair<V>>();
		Sink<IntObjPair<V>> enqueue = pair -> enqueue(queue, pair);

		var thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				var p = queue.take();
				var b = p != null;
				if (b)
					pair.update(p.t0, p.t1);
				return b;
			} catch (InterruptedException ex) {
				thread.interrupt();
				return Fail.t();
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
