package suite.primitive;

import java.util.Collections;
import java.util.Iterator;

import suite.adt.pair.IntObjPair;
import suite.os.LogUtil;
import suite.primitive.IntFun.Int_Obj;
import suite.primitive.IntPredicate.IntPredicate_;
import suite.primitive.IntSink.IntSink_;
import suite.primitive.IntSource.IntSource_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class IntFunUtil {

	public static int EMPTYVALUE = Integer.MIN_VALUE;

	public static IntSource_ append(int t, IntSource_ source) {
		return new IntSource_() {
			private boolean isAppended = false;

			public int source() {
				if (!isAppended) {
					int t_ = source.source();
					if (t_ != EMPTYVALUE)
						return t_;
					else {
						isAppended = true;
						return t;
					}
				} else
					return EMPTYVALUE;
			}
		};
	}

	public static Source<IntSource_> chunk(int n, IntSource_ source) {
		return new Source<IntSource_>() {
			private int t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private int i;
			private IntSource_ source_ = () -> {
				if ((isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && ++i < n)
					return t;
				else {
					i = 0;
					return EMPTYVALUE;
				}
			};

			public IntSource_ source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	public static IntSource_ concat(Source<IntSource_> source) {
		return new IntSource_() {
			private IntSource_ source0 = nullSource();

			public int source() {
				int t = EMPTYVALUE;
				while (source0 != null && (t = source0.source()) == EMPTYVALUE)
					source0 = source.source();
				return t;
			}
		};
	}

	public static IntSource_ cons(int t, IntSource_ source) {
		return new IntSource_() {
			private boolean isFirst = true;

			public int source() {
				if (!isFirst)
					return source.source();
				else {
					isFirst = false;
					return t;
				}
			}
		};
	}

	public static IntSource_ filter(IntPredicate_ fun0, IntSource_ source) {
		IntPredicate_ fun1 = fun0.rethrow();
		return () -> {
			int t = EMPTYVALUE;
			while ((t = source.source()) != EMPTYVALUE && !fun1.test(t))
				;
			return t;
		};
	}

	public static IntSource_ flatten(Source<Iterable<Integer>> source) {
		return new IntSource_() {
			private Iterator<Integer> iter = Collections.emptyIterator();

			public int source() {
				Iterable<Integer> iterable;
				while (!iter.hasNext())
					if ((iterable = source.source()) != null)
						iter = iterable.iterator();
					else
						return EMPTYVALUE;
				return iter.next();
			}
		};
	}

	public static <R> R fold(Fun<IntObjPair<R>, R> fun0, R init, IntSource_ source) {
		Fun<IntObjPair<R>, R> fun1 = Rethrow.fun(fun0);
		int t;
		while ((t = source.source()) != EMPTYVALUE)
			init = fun1.apply(IntObjPair.of(t, init));
		return init;
	}

	public static boolean isAll(IntPredicate_ pred0, IntSource_ source) {
		IntPredicate_ pred1 = pred0.rethrow();
		int t;
		while ((t = source.source()) != EMPTYVALUE)
			if (!pred1.test(t))
				return false;
		return true;
	}

	public static boolean isAny(IntPredicate_ pred0, IntSource_ source) {
		IntPredicate_ pred1 = pred0.rethrow();
		int t;
		while ((t = source.source()) != EMPTYVALUE)
			if (pred1.test(t))
				return true;
		return false;
	}

	public static Iterator<Integer> iterator(IntSource_ source) {
		return new Iterator<Integer>() {
			private int next = EMPTYVALUE;

			public boolean hasNext() {
				if (next == EMPTYVALUE)
					next = source.source();
				return next != EMPTYVALUE;
			}

			public Integer next() {
				int next0 = next;
				next = EMPTYVALUE;
				return next0;
			}

		};
	}

	public static Iterable<Integer> iter(IntSource_ source) {
		return () -> iterator(source);
	}

	public static <T0, T1> Source<T1> map(Fun<T0, T1> fun0, Source<T0> source) {
		Fun<T0, T1> fun1 = Rethrow.fun(fun0);
		return () -> {
			T0 t0 = source.source();
			return t0 != null ? fun1.apply(t0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Int_Obj<K> kf0, Int_Obj<V> vf0, IntSource_ source) {
		Int_Obj<K> kf1 = kf0.rethrow();
		Int_Obj<V> vf1 = vf0.rethrow();
		return pair -> {
			int t = source.source();
			boolean b = t != EMPTYVALUE;
			if (b) {
				pair.t0 = kf1.apply(t);
				pair.t1 = vf1.apply(t);
			}
			return b;
		};
	}

	public static <T0, T1> Source<T1> mapNonNull(Fun<T0, T1> fun, Source<T0> source) {
		return new Source<T1>() {
			public T1 source() {
				T0 t0;
				T1 t1;
				while ((t0 = source.source()) != null)
					if ((t1 = fun.apply(t0)) != null)
						return t1;
				return null;
			}
		};
	}

	public static IntSink_ nullSink() {
		return i -> {
		};
	}

	public static IntSource_ nullSource() {
		return () -> EMPTYVALUE;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<IntSource_> split(IntPredicate_ fun0, IntSource_ source) {
		IntPredicate_ fun1 = fun0.rethrow();
		return new Source<IntSource_>() {
			private int t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private IntSource_ source_ = () -> (isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && !fun1.test(t) ? t
					: null;

			public IntSource_ source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static IntSource_ suck(Sink<IntSink_> fun) {
		NullableSyncQueue<Integer> queue = new NullableSyncQueue<>();
		IntSink_ enqueue = t -> enqueue(queue, t);

		Thread thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, EMPTYVALUE);
			}
		});

		return () -> {
			try {
				return queue.take();
			} catch (InterruptedException ex) {
				thread.interrupt();
				throw new RuntimeException(ex);
			}
		};
	}

	private static void enqueue(NullableSyncQueue<Integer> queue, int t) {
		try {
			queue.offer(t);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
		}
	}

}
