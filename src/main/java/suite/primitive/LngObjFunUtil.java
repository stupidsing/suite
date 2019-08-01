package suite.primitive;

import static primal.statics.Fail.fail;

import java.util.Iterator;
import java.util.function.Predicate;

import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Source2;
import primal.os.Log_;
import primal.primitive.LngPrim.LngTest;
import primal.statics.Fail.InterruptedRuntimeException;
import primal.statics.Rethrow;
import suite.primitive.LngPrimitives.LngObjPredicate;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.LngObj_Obj;
import suite.primitive.adt.pair.LngObjPair;
import suite.util.NullableSyncQueue;
import suite.util.Thread_;

public class LngObjFunUtil {

	public static <V> Source<LngObjSource<V>> chunk(int n, LngObjSource<V> source) {
		return new Source<>() {
			private LngObjPair<V> pair;
			private boolean isAvail;
			private int i;
			private LngObjSource<V> source_ = pair1 -> {
				var b = (isAvail = isAvail && source.source2(pair)) && ++i < n;
				if (b)
					pair1.update(pair.k, pair.v);
				else
					i = 0;
				return b;
			};

			{
				isAvail = source.source2(pair);
			}

			public LngObjSource<V> g() {
				return isAvail ? cons(pair.k, pair.v, source_) : null;
			}
		};
	}

	public static <V> LngObjSource<V> concat(Source<LngObjSource<V>> source) {
		return new LngObjSource<>() {
			private LngObjSource<V> source2 = nullSource();

			public boolean source2(LngObjPair<V> pair) {
				var b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.g();
				return b;
			}
		};
	}

	public static <V> LngObjSource<V> cons(long key, V value, LngObjSource<V> source2) {
		return new LngObjSource<>() {
			private boolean isFirst = true;

			public boolean source2(LngObjPair<V> pair) {
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

	public static <V> LngObjSource<V> filter(LngObjPredicate<V> fun0, LngObjSource<V> source2) {
		var fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.k, pair.v))
				;
			return b;
		};
	}

	public static <V> LngObjSource<V> filterKey(LngTest fun0, LngObjSource<V> source2) {
		var fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.k))
				;
			return b;
		};
	}

	public static <V> LngObjSource<V> filterValue(Predicate<V> fun0, LngObjSource<V> source2) {
		var fun1 = Rethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.v))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, LngObjPair<V>>, R> fun0, R init, LngObjSource<V> source2) {
		var fun1 = fun0.rethrow();
		LngObjPair<V> pair = LngObjPair.of((long) 0, null);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static <V> boolean isAll(LngObjPredicate<V> pred0, LngObjSource<V> source2) {
		var pred1 = pred0.rethrow();
		LngObjPair<V> pair = LngObjPair.of((long) 0, null);
		while (source2.source2(pair))
			if (!pred1.test(pair.k, pair.v))
				return false;
		return true;
	}

	public static <V> boolean isAny(LngObjPredicate<V> pred0, LngObjSource<V> source2) {
		var pred1 = pred0.rethrow();
		LngObjPair<V> pair = LngObjPair.of((long) 0, null);
		while (source2.source2(pair))
			if (pred1.test(pair.k, pair.v))
				return true;
		return false;
	}

	public static <V> Iterator<LngObjPair<V>> iterator(LngObjSource<V> source2) {
		return new Iterator<>() {
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
				var next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<LngObjPair<V>> iter(LngObjSource<V> source2) {
		return () -> iterator(source2);
	}

	public static <V, T> Source<T> map(LngObj_Obj<V, T> fun0, LngObjSource<V> source2) {
		var fun1 = fun0.rethrow();
		LngObjPair<V> pair = LngObjPair.of((long) 0, null);
		return () -> source2.source2(pair) ? fun1.apply(pair.k, pair.v) : null;
	}

	public static <V, K1, V1, T> Source2<K1, V1> map2(LngObj_Obj<V, K1> kf0, LngObj_Obj<V, V1> vf0, LngObjSource<V> source2) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		LngObjPair<V> pair1 = LngObjPair.of((long) 0, null);
		return pair -> {
			var b = source2.source2(pair1);
			if (b)
				pair.update(kf1.apply(pair1.k, pair1.v), vf1.apply(pair1.k, pair1.v));
			return b;
		};
	}

	public static <V, V1, T> LngObjSource<V1> mapLngObj(LngObj_Lng<V> kf0, LngObj_Obj<V, V1> vf0, LngObjSource<V> source2) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		LngObjPair<V> pair1 = LngObjPair.of((long) 0, null);
		return pair -> {
			var b = source2.source2(pair1);
			if (b)
				pair.update(kf1.apply(pair1.k, pair1.v), vf1.apply(pair1.k, pair1.v));
			return b;
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
	 * Problematic split: all data must be read, i.e. the children lists must not be
	 * skipped.
	 */
	public static <V> Source<LngObjSource<V>> split(LngObjPredicate<V> fun0, LngObjSource<V> source2) {
		LngObjPredicate<V> fun1 = fun0.rethrow();
		return new Source<>() {
			private LngObjPair<V> pair = LngObjPair.of((long) 0, null);
			private boolean isAvailable;
			private LngObjSource<V> source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.k, pair.v);

			{
				isAvailable = source2.source2(pair);
			}

			public LngObjSource<V> g() {
				return isAvailable ? cons(pair.k, pair.v, source2_) : null;
			}
		};
	}

	public static <V> LngObjSource<V> snoc(long key, V value, LngObjSource<V> source) {
		return new LngObjSource<>() {
			private boolean isAppended = false;

			public boolean source2(LngObjPair<V> pair) {
				var b = !isAppended;
				if (b && !source.source2(pair)) {
					pair.update(key, value);
					isAppended = true;
				}
				return b;
			}
		};
	}

	/**
	 * Sucks data from a sink and make it into a source.
	 */
	public static <V> LngObjSource<V> suck(Sink<Sink<LngObjPair<V>>> fun) {
		var queue = new NullableSyncQueue<LngObjPair<V>>();
		Sink<LngObjPair<V>> enqueue = pair -> enqueue(queue, pair);

		var thread = Thread_.startThread(() -> {
			try {
				fun.f(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				var p = queue.take();
				var b = p != null;
				if (b)
					pair.update(p.k, p.v);
				return b;
			} catch (InterruptedException | InterruptedRuntimeException ex) {
				thread.interrupt();
				return fail();
			}
		};
	}

	private static <T> void enqueue(NullableSyncQueue<T> queue, T t) {
		try {
			queue.offer(t);
		} catch (InterruptedException ex) {
			Log_.error(ex);
		}
	}

}
