package suite.primitive;

import java.util.Collections;
import java.util.Iterator;

import suite.adt.pair.LngObjPair;
import suite.os.LogUtil;
import suite.primitive.LngFun.Lng_Obj;
import suite.primitive.LngPredicate.LngPredicate_;
import suite.primitive.LngSink.LngSink_;
import suite.primitive.LngSource.LngSource_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class LngFunUtil {

	public static long EMPTYVALUE = Long.MIN_VALUE;

	public static LngSource_ append(long t, LngSource_ source) {
		return new LngSource_() {
			private boolean isAppended = false;

			public long source() {
				if (!isAppended) {
					long t_ = source.source();
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

	public static Source<LngSource_> chunk(int n, LngSource_ source) {
		return new Source<LngSource_>() {
			private long t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private int i;
			private LngSource_ source_ = () -> {
				if ((isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && ++i < n)
					return t;
				else {
					i = 0;
					return EMPTYVALUE;
				}
			};

			public LngSource_ source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	public static LngSource_ concat(Source<LngSource_> source) {
		return new LngSource_() {
			private LngSource_ source0 = nullSource();

			public long source() {
				long t = EMPTYVALUE;
				while (source0 != null && (t = source0.source()) == EMPTYVALUE)
					source0 = source.source();
				return t;
			}
		};
	}

	public static LngSource_ cons(long t, LngSource_ source) {
		return new LngSource_() {
			private boolean isFirst = true;

			public long source() {
				if (!isFirst)
					return source.source();
				else {
					isFirst = false;
					return t;
				}
			}
		};
	}

	public static LngSource_ filter(LngPredicate_ fun0, LngSource_ source) {
		LngPredicate_ fun1 = fun0.rethrow();
		return () -> {
			long t = EMPTYVALUE;
			while ((t = source.source()) != EMPTYVALUE && !fun1.test(t))
				;
			return t;
		};
	}

	public static LngSource_ flatten(Source<Iterable<Long>> source) {
		return new LngSource_() {
			private Iterator<Long> iter = Collections.emptyIterator();

			public long source() {
				Iterable<Long> iterable;
				while (!iter.hasNext())
					if ((iterable = source.source()) != null)
						iter = iterable.iterator();
					else
						return EMPTYVALUE;
				return iter.next();
			}
		};
	}

	public static <R> R fold(Fun<LngObjPair<R>, R> fun0, R init, LngSource_ source) {
		Fun<LngObjPair<R>, R> fun1 = Rethrow.fun(fun0);
		long t;
		while ((t = source.source()) != EMPTYVALUE)
			init = fun1.apply(LngObjPair.of(t, init));
		return init;
	}

	public static boolean isAll(LngPredicate_ pred0, LngSource_ source) {
		LngPredicate_ pred1 = pred0.rethrow();
		long t;
		while ((t = source.source()) != EMPTYVALUE)
			if (!pred1.test(t))
				return false;
		return true;
	}

	public static boolean isAny(LngPredicate_ pred0, LngSource_ source) {
		LngPredicate_ pred1 = pred0.rethrow();
		long t;
		while ((t = source.source()) != EMPTYVALUE)
			if (pred1.test(t))
				return true;
		return false;
	}

	public static Iterator<Long> iterator(LngSource_ source) {
		return new Iterator<Long>() {
			private long next = EMPTYVALUE;

			public boolean hasNext() {
				if (next == EMPTYVALUE)
					next = source.source();
				return next != EMPTYVALUE;
			}

			public Long next() {
				long next0 = next;
				next = EMPTYVALUE;
				return next0;
			}

		};
	}

	public static Iterable<Long> iter(LngSource_ source) {
		return () -> iterator(source);
	}

	public static <T0, T1> Source<T1> map(Fun<T0, T1> fun0, Source<T0> source) {
		Fun<T0, T1> fun1 = Rethrow.fun(fun0);
		return () -> {
			T0 t0 = source.source();
			return t0 != null ? fun1.apply(t0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Lng_Obj<K> kf0, Lng_Obj<V> vf0, LngSource_ source) {
		Lng_Obj<K> kf1 = kf0.rethrow();
		Lng_Obj<V> vf1 = vf0.rethrow();
		return pair -> {
			long t = source.source();
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

	public static LngSink_ nullSink() {
		return i -> {
		};
	}

	public static LngSource_ nullSource() {
		return () -> EMPTYVALUE;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<LngSource_> split(LngPredicate_ fun0, LngSource_ source) {
		LngPredicate_ fun1 = fun0.rethrow();
		return new Source<LngSource_>() {
			private long t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private LngSource_ source_ = () -> (isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && !fun1.test(t) ? t
					: null;

			public LngSource_ source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static LngSource_ suck(Sink<LngSink_> fun) {
		NullableSyncQueue<Long> queue = new NullableSyncQueue<>();
		LngSink_ enqueue = t -> enqueue(queue, t);

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

	private static void enqueue(NullableSyncQueue<Long> queue, long t) {
		try {
			queue.offer(t);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
		}
	}

}
