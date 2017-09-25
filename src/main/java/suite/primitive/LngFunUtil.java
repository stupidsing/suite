package suite.primitive;

import java.util.Collections;
import java.util.Iterator;

import suite.os.LogUtil;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.LngPredicate;
import suite.primitive.LngPrimitives.LngSink;
import suite.primitive.LngPrimitives.LngSource;
import suite.primitive.LngPrimitives.Lng_Obj;
import suite.primitive.adt.pair.LngObjPair;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Thread_;

public class LngFunUtil {

	public static long EMPTYVALUE = Long.MIN_VALUE;

	public static LngSource append(long c, LngSource source) {
		return new LngSource() {
			private boolean isAppended = false;

			public long source() {
				if (!isAppended) {
					long c_ = source.source();
					if (c_ != EMPTYVALUE)
						return c_;
					else {
						isAppended = true;
						return c;
					}
				} else
					return EMPTYVALUE;
			}
		};
	}

	public static Source<LngSource> chunk(int n, LngSource source) {
		return new Source<>() {
			private long c = source.source();
			private boolean isAvail = c != EMPTYVALUE;
			private int i;
			private LngSource source_ = () -> {
				if ((isAvail = isAvail && (c = source.source()) != EMPTYVALUE) && ++i < n)
					return c;
				else {
					i = 0;
					return EMPTYVALUE;
				}
			};

			public LngSource source() {
				return isAvail ? cons(c, source_) : null;
			}
		};
	}

	public static LngSource concat(Source<LngSource> source) {
		return new LngSource() {
			private LngSource source0 = nullSource();

			public long source() {
				long c = EMPTYVALUE;
				while (source0 != null && (c = source0.source()) == EMPTYVALUE)
					source0 = source.source();
				return c;
			}
		};
	}

	public static LngSource cons(long c, LngSource source) {
		return new LngSource() {
			private boolean isFirst = true;

			public long source() {
				if (!isFirst)
					return source.source();
				else {
					isFirst = false;
					return c;
				}
			}
		};
	}

	public static LngSource filter(LngPredicate fun0, LngSource source) {
		LngPredicate fun1 = fun0.rethrow();
		return () -> {
			long c = EMPTYVALUE;
			while ((c = source.source()) != EMPTYVALUE && !fun1.test(c))
				;
			return c;
		};
	}

	public static LngSource flatten(Source<Iterable<Long>> source) {
		return new LngSource() {
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

	public static <R> R fold(Fun<LngObjPair<R>, R> fun0, R init, LngSource source) {
		Fun<LngObjPair<R>, R> fun1 = fun0.rethrow();
		long c;
		while ((c = source.source()) != EMPTYVALUE)
			init = fun1.apply(LngObjPair.of(c, init));
		return init;
	}

	public static boolean isAll(LngPredicate pred0, LngSource source) {
		LngPredicate pred1 = pred0.rethrow();
		long c;
		while ((c = source.source()) != EMPTYVALUE)
			if (!pred1.test(c))
				return false;
		return true;
	}

	public static boolean isAny(LngPredicate pred0, LngSource source) {
		LngPredicate pred1 = pred0.rethrow();
		long c;
		while ((c = source.source()) != EMPTYVALUE)
			if (pred1.test(c))
				return true;
		return false;
	}

	public static Iterator<Long> iterator(LngSource source) {
		return new Iterator<>() {
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

	public static Iterable<Long> iter(LngSource source) {
		return () -> iterator(source);
	}

	public static <T1> Source<T1> map(Lng_Obj<T1> fun0, LngSource source) {
		Lng_Obj<T1> fun1 = fun0.rethrow();
		return () -> {
			long c0 = source.source();
			return c0 != LngFunUtil.EMPTYVALUE ? fun1.apply(c0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Lng_Obj<K> kf0, Lng_Obj<V> vf0, LngSource source) {
		Lng_Obj<K> kf1 = kf0.rethrow();
		Lng_Obj<V> vf1 = vf0.rethrow();
		return pair -> {
			long c = source.source();
			boolean b = c != EMPTYVALUE;
			if (b)
				pair.update(kf1.apply(c), vf1.apply(c));
			return b;
		};
	}

	public static LngSource mapLng(Lng_Lng fun0, LngSource source) {
		Lng_Lng fun1 = fun0.rethrow();
		return () -> {
			long c = source.source();
			return c != LngFunUtil.EMPTYVALUE ? fun1.apply(c) : LngFunUtil.EMPTYVALUE;
		};
	}

	public static <V> LngObjSource<V> mapLngObj(Lng_Obj<V> fun0, LngSource source) {
		Lng_Obj<V> fun1 = fun0.rethrow();
		return pair -> {
			long c = source.source();
			if (c != LngFunUtil.EMPTYVALUE) {
				pair.update(c, fun1.apply(c));
				return true;
			} else
				return false;

		};
	}

	public static LngSink nullSink() {
		return i -> {
		};
	}

	public static LngSource nullSource() {
		return () -> EMPTYVALUE;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<LngSource> split(LngPredicate fun0, LngSource source) {
		LngPredicate fun1 = fun0.rethrow();
		return new Source<>() {
			private long c = source.source();
			private boolean isAvail = c != EMPTYVALUE;
			private LngSource source_ = () -> (isAvail = isAvail && (c = source.source()) != EMPTYVALUE) && !fun1.test(c) ? c
					: null;

			public LngSource source() {
				return isAvail ? cons(c, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static LngSource suck(Sink<LngSink> fun) {
		NullableSyncQueue<Long> queue = new NullableSyncQueue<>();
		LngSink enqueue = c -> enqueue(queue, c);

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

	private static void enqueue(NullableSyncQueue<Long> queue, long c) {
		try {
			queue.offer(c);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
		}
	}

}
