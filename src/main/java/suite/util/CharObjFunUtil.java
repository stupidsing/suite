package suite.util;

import java.util.Iterator;
import java.util.function.Predicate;

import suite.adt.pair.CharObjPair;
import suite.adt.pair.Pair;
import suite.os.LogUtil;
import suite.primitive.CharPrimitiveFun.CharObj_Char;
import suite.primitive.CharPrimitiveFun.CharObj_Obj;
import suite.primitive.CharPrimitivePredicate.CharObjPredicate;
import suite.primitive.CharPrimitivePredicate.CharPredicate_;
import suite.primitive.CharPrimitiveSource.CharObjSource;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;

public class CharObjFunUtil {

	public static <V> CharObjSource<V> append(char key, V value, CharObjSource<V> source) {
		return new CharObjSource<V>() {
			private boolean isAppended = false;

			public boolean source2(CharObjPair<V> pair) {
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

	public static <V> Source<CharObjSource<V>> chunk(int n, CharObjSource<V> source) {
		return new Source<CharObjSource<V>>() {
			private CharObjPair<V> pair;
			private boolean isAvail;
			private int i;
			private CharObjSource<V> source_ = pair1 -> {
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

			public CharObjSource<V> source() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> CharObjSource<V> concat(Source<CharObjSource<V>> source) {
		return new CharObjSource<V>() {
			private CharObjSource<V> source2 = nullSource();

			public boolean source2(CharObjPair<V> pair) {
				boolean b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.source();
				return b;
			}
		};
	}

	public static <V> CharObjSource<V> cons(char key, V value, CharObjSource<V> source2) {
		return new CharObjSource<V>() {
			private boolean isFirst = true;

			public boolean source2(CharObjPair<V> pair) {
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

	public static <V> CharObjSource<V> filter(CharObjPredicate<V> fun0, CharObjSource<V> source2) {
		CharObjPredicate<V> fun1 = CharRethrow.charObjPredicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> CharObjSource<V> filterKey(CharPredicate_ fun0, CharObjSource<V> source2) {
		CharPredicate_ fun1 = CharRethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static <V> CharObjSource<V> filterValue(Predicate<V> fun0, CharObjSource<V> source2) {
		Predicate<V> fun1 = Rethrow.predicate(fun0);
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, CharObjPair<V>>, R> fun0, R init, CharObjSource<V> source2) {
		Fun<Pair<R, CharObjPair<V>>, R> fun1 = Rethrow.fun(fun0);
		CharObjPair<V> pair = CharObjPair.of((char) 0, null);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static <V> boolean isAll(CharObjPredicate<V> pred0, CharObjSource<V> source2) {
		CharObjPredicate<V> pred1 = CharRethrow.charObjPredicate(pred0);
		CharObjPair<V> pair = CharObjPair.of((char) 0, null);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(CharObjPredicate<V> pred0, CharObjSource<V> source2) {
		CharObjPredicate<V> pred1 = CharRethrow.charObjPredicate(pred0);
		CharObjPair<V> pair = CharObjPair.of((char) 0, null);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<CharObjPair<V>> iterator(CharObjSource<V> source2) {
		return new Iterator<CharObjPair<V>>() {
			private CharObjPair<V> next = null;

			public boolean hasNext() {
				if (next == null) {
					CharObjPair<V> next1 = CharObjPair.of((char) 0, null);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public CharObjPair<V> next() {
				CharObjPair<V> next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<CharObjPair<V>> iter(CharObjSource<V> source2) {
		return () -> iterator(source2);
	}

	public static <V, T> Source<T> map(CharObj_Obj<V, T> fun0, CharObjSource<V> source2) {
		CharObj_Obj<V, T> fun1 = CharRethrow.fun2(fun0);
		CharObjPair<V> pair = CharObjPair.of((char) 0, null);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <V, K1, V1, T> Source2<K1, V1> map2(CharObj_Obj<V, K1> kf0, CharObj_Obj<V, V1> vf0, CharObjSource<V> source2) {
		CharObj_Obj<V, K1> kf1 = CharRethrow.fun2(kf0);
		CharObj_Obj<V, V1> vf1 = CharRethrow.fun2(vf0);
		CharObjPair<V> pair1 = CharObjPair.of((char) 0, null);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <V, V1, T> CharObjSource<V1> mapCharObj(CharObj_Char<V> kf0, CharObj_Obj<V, V1> vf0, CharObjSource<V> source2) {
		CharObj_Char<V> kf1 = CharRethrow.fun2(kf0);
		CharObj_Obj<V, V1> vf1 = CharRethrow.fun2(vf0);
		CharObjPair<V> pair1 = CharObjPair.of((char) 0, null);
		return pair -> {
			boolean b = source2.source2(pair1);
			if (b) {
				pair.t0 = kf1.apply(pair1.t0, pair1.t1);
				pair.t1 = vf1.apply(pair1.t0, pair1.t1);
			}
			return b;
		};
	}

	public static <V, T1> Source<T1> mapNonNull(CharObj_Obj<V, T1> fun, CharObjSource<V> source) {
		return new Source<T1>() {
			public T1 source() {
				CharObjPair<V> pair = CharObjPair.of((char) 0, null);
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

	public static <V> CharObjSource<V> nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static <V> Source<CharObjSource<V>> split(CharObjPredicate<V> fun0, CharObjSource<V> source2) {
		CharObjPredicate<V> fun1 = CharRethrow.charObjPredicate(fun0);
		return new Source<CharObjSource<V>>() {
			private CharObjPair<V> pair = CharObjPair.of((char) 0, null);
			private boolean isAvailable;
			private CharObjSource<V> source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public CharObjSource<V> source() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <V> CharObjSource<V> suck(Sink<Sink<CharObjPair<V>>> fun) {
		NullableSynchronousQueue<CharObjPair<V>> queue = new NullableSynchronousQueue<>();
		Sink<CharObjPair<V>> enqueue = pair -> enqueue(queue, pair);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				CharObjPair<V> p = queue.take();
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

	private static <T> void enqueue(NullableSynchronousQueue<T> queue, T t) {
		try {
			queue.offer(t);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
		}
	}

}
