package suite.primitive;

import java.util.Collections;
import java.util.Iterator;

import suite.adt.pair.IntObjPair;
import suite.os.LogUtil;
import suite.primitive.IntPrimitives.IntPredicate;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.IntPrimitives.IntSource;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class IntFunUtil {

	public static int EMPTYVALUE = Integer.MIN_VALUE;

	public static IntSource append(int t, IntSource source) {
		return new IntSource() {
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

	public static Source<IntSource> chunk(int n, IntSource source) {
		return new Source<IntSource>() {
			private int t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private int i;
			private IntSource source_ = () -> {
				if ((isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && ++i < n)
					return t;
				else {
					i = 0;
					return EMPTYVALUE;
				}
			};

			public IntSource source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	public static IntSource concat(Source<IntSource> source) {
		return new IntSource() {
			private IntSource source0 = nullSource();

			public int source() {
				int t = EMPTYVALUE;
				while (source0 != null && (t = source0.source()) == EMPTYVALUE)
					source0 = source.source();
				return t;
			}
		};
	}

	public static IntSource cons(int t, IntSource source) {
		return new IntSource() {
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

	public static IntSource filter(IntPredicate fun0, IntSource source) {
		IntPredicate fun1 = fun0.rethrow();
		return () -> {
			int t = EMPTYVALUE;
			while ((t = source.source()) != EMPTYVALUE && !fun1.test(t))
				;
			return t;
		};
	}

	public static IntSource flatten(Source<Iterable<Integer>> source) {
		return new IntSource() {
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

	public static <R> R fold(Fun<IntObjPair<R>, R> fun0, R init, IntSource source) {
		Fun<IntObjPair<R>, R> fun1 = Rethrow.fun(fun0);
		int t;
		while ((t = source.source()) != EMPTYVALUE)
			init = fun1.apply(IntObjPair.of(t, init));
		return init;
	}

	public static boolean isAll(IntPredicate pred0, IntSource source) {
		IntPredicate pred1 = pred0.rethrow();
		int t;
		while ((t = source.source()) != EMPTYVALUE)
			if (!pred1.test(t))
				return false;
		return true;
	}

	public static boolean isAny(IntPredicate pred0, IntSource source) {
		IntPredicate pred1 = pred0.rethrow();
		int t;
		while ((t = source.source()) != EMPTYVALUE)
			if (pred1.test(t))
				return true;
		return false;
	}

	public static Iterator<Integer> iterator(IntSource source) {
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

	public static Iterable<Integer> iter(IntSource source) {
		return () -> iterator(source);
	}

	public static <T1> Source<T1> map(Int_Obj<T1> fun0, IntSource source) {
		Int_Obj<T1> fun1 = fun0.rethrow();
		return () -> {
			int t0 = source.source();
			return t0 != IntFunUtil.EMPTYVALUE ? fun1.apply(t0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Int_Obj<K> kf0, Int_Obj<V> vf0, IntSource source) {
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

	public static IntSource mapInt(Int_Int fun0, IntSource source) {
		Int_Int fun1 = fun0.rethrow();
		return () -> {
			int t = source.source();
			return t != IntFunUtil.EMPTYVALUE ? fun1.apply(t) : IntFunUtil.EMPTYVALUE;
		};
	}

	public static <T> Source<T> mapNonNull(Int_Obj<T> fun0, IntSource source) {
		Int_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				int t0;
				T t1;
				while ((t0 = source.source()) != IntFunUtil.EMPTYVALUE)
					if ((t1 = fun1.apply(t0)) != null)
						return t1;
				return null;
			}
		};
	}

	public static IntSink nullSink() {
		return i -> {
		};
	}

	public static IntSource nullSource() {
		return () -> EMPTYVALUE;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<IntSource> split(IntPredicate fun0, IntSource source) {
		IntPredicate fun1 = fun0.rethrow();
		return new Source<IntSource>() {
			private int t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private IntSource source_ = () -> (isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && !fun1.test(t) ? t
					: null;

			public IntSource source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static IntSource suck(Sink<IntSink> fun) {
		NullableSyncQueue<Integer> queue = new NullableSyncQueue<>();
		IntSink enqueue = t -> enqueue(queue, t);

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
