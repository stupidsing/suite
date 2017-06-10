package suite.primitive;

import java.util.Collections;
import java.util.Iterator;

import suite.adt.pair.FltObjPair;
import suite.os.LogUtil;
import suite.primitive.FltPrimitives.FltPredicate;
import suite.primitive.FltPrimitives.FltSink;
import suite.primitive.FltPrimitives.FltSource;
import suite.primitive.FltPrimitives.Flt_Obj;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class FltFunUtil {

	public static float EMPTYVALUE = Float.MIN_VALUE;

	public static FltSource append(float t, FltSource source) {
		return new FltSource() {
			private boolean isAppended = false;

			public float source() {
				if (!isAppended) {
					float t_ = source.source();
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

	public static Source<FltSource> chunk(int n, FltSource source) {
		return new Source<FltSource>() {
			private float t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private int i;
			private FltSource source_ = () -> {
				if ((isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && ++i < n)
					return t;
				else {
					i = 0;
					return EMPTYVALUE;
				}
			};

			public FltSource source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	public static FltSource concat(Source<FltSource> source) {
		return new FltSource() {
			private FltSource source0 = nullSource();

			public float source() {
				float t = EMPTYVALUE;
				while (source0 != null && (t = source0.source()) == EMPTYVALUE)
					source0 = source.source();
				return t;
			}
		};
	}

	public static FltSource cons(float t, FltSource source) {
		return new FltSource() {
			private boolean isFirst = true;

			public float source() {
				if (!isFirst)
					return source.source();
				else {
					isFirst = false;
					return t;
				}
			}
		};
	}

	public static FltSource filter(FltPredicate fun0, FltSource source) {
		FltPredicate fun1 = fun0.rethrow();
		return () -> {
			float t = EMPTYVALUE;
			while ((t = source.source()) != EMPTYVALUE && !fun1.test(t))
				;
			return t;
		};
	}

	public static FltSource flatten(Source<Iterable<Float>> source) {
		return new FltSource() {
			private Iterator<Float> iter = Collections.emptyIterator();

			public float source() {
				Iterable<Float> iterable;
				while (!iter.hasNext())
					if ((iterable = source.source()) != null)
						iter = iterable.iterator();
					else
						return EMPTYVALUE;
				return iter.next();
			}
		};
	}

	public static <R> R fold(Fun<FltObjPair<R>, R> fun0, R init, FltSource source) {
		Fun<FltObjPair<R>, R> fun1 = Rethrow.fun(fun0);
		float t;
		while ((t = source.source()) != EMPTYVALUE)
			init = fun1.apply(FltObjPair.of(t, init));
		return init;
	}

	public static boolean isAll(FltPredicate pred0, FltSource source) {
		FltPredicate pred1 = pred0.rethrow();
		float t;
		while ((t = source.source()) != EMPTYVALUE)
			if (!pred1.test(t))
				return false;
		return true;
	}

	public static boolean isAny(FltPredicate pred0, FltSource source) {
		FltPredicate pred1 = pred0.rethrow();
		float t;
		while ((t = source.source()) != EMPTYVALUE)
			if (pred1.test(t))
				return true;
		return false;
	}

	public static Iterator<Float> iterator(FltSource source) {
		return new Iterator<Float>() {
			private float next = EMPTYVALUE;

			public boolean hasNext() {
				if (next == EMPTYVALUE)
					next = source.source();
				return next != EMPTYVALUE;
			}

			public Float next() {
				float next0 = next;
				next = EMPTYVALUE;
				return next0;
			}

		};
	}

	public static Iterable<Float> iter(FltSource source) {
		return () -> iterator(source);
	}

	public static <T1> Source<T1> map(Flt_Obj<T1> fun0, FltSource source) {
		Flt_Obj<T1> fun1 = fun0.rethrow();
		return () -> {
			float t0 = source.source();
			return t0 != FltFunUtil.EMPTYVALUE ? fun1.apply(t0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Flt_Obj<K> kf0, Flt_Obj<V> vf0, FltSource source) {
		Flt_Obj<K> kf1 = kf0.rethrow();
		Flt_Obj<V> vf1 = vf0.rethrow();
		return pair -> {
			float t = source.source();
			boolean b = t != EMPTYVALUE;
			if (b) {
				pair.t0 = kf1.apply(t);
				pair.t1 = vf1.apply(t);
			}
			return b;
		};
	}

	public static FltSource mapFlt(Flt_Flt fun0, FltSource source) {
		Flt_Flt fun1 = fun0.rethrow();
		return () -> {
			float t = source.source();
			return t != FltFunUtil.EMPTYVALUE ? fun1.apply(t) : FltFunUtil.EMPTYVALUE;
		};
	}

	public static <T> Source<T> mapNonNull(Flt_Obj<T> fun0, FltSource source) {
		Flt_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				float t0;
				T t1;
				while ((t0 = source.source()) != FltFunUtil.EMPTYVALUE)
					if ((t1 = fun1.apply(t0)) != null)
						return t1;
				return null;
			}
		};
	}

	public static FltSink nullSink() {
		return i -> {
		};
	}

	public static FltSource nullSource() {
		return () -> EMPTYVALUE;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<FltSource> split(FltPredicate fun0, FltSource source) {
		FltPredicate fun1 = fun0.rethrow();
		return new Source<FltSource>() {
			private float t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private FltSource source_ = () -> (isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && !fun1.test(t) ? t
					: null;

			public FltSource source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static FltSource suck(Sink<FltSink> fun) {
		NullableSyncQueue<Float> queue = new NullableSyncQueue<>();
		FltSink enqueue = t -> enqueue(queue, t);

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

	private static void enqueue(NullableSyncQueue<Float> queue, float t) {
		try {
			queue.offer(t);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
		}
	}

}
