package suite.primitive;

import java.util.Collections;
import java.util.Iterator;

import suite.adt.pair.FltObjPair;
import suite.os.LogUtil;
import suite.primitive.FltFun.Flt_Obj;
import suite.primitive.FltPredicate.FltPredicate_;
import suite.primitive.FltSink.FltSink_;
import suite.primitive.FltSource.FltSource_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class FltFunUtil {

	public static float EMPTYVALUE = Float.MIN_VALUE;

	public static FltSource_ append(float t, FltSource_ source) {
		return new FltSource_() {
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

	public static Source<FltSource_> chunk(int n, FltSource_ source) {
		return new Source<FltSource_>() {
			private float t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private int i;
			private FltSource_ source_ = () -> {
				if ((isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && ++i < n)
					return t;
				else {
					i = 0;
					return EMPTYVALUE;
				}
			};

			public FltSource_ source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	public static FltSource_ concat(Source<FltSource_> source) {
		return new FltSource_() {
			private FltSource_ source0 = nullSource();

			public float source() {
				float t = EMPTYVALUE;
				while (source0 != null && (t = source0.source()) == EMPTYVALUE)
					source0 = source.source();
				return t;
			}
		};
	}

	public static FltSource_ cons(float t, FltSource_ source) {
		return new FltSource_() {
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

	public static FltSource_ filter(FltPredicate_ fun0, FltSource_ source) {
		FltPredicate_ fun1 = fun0.rethrow();
		return () -> {
			float t = EMPTYVALUE;
			while ((t = source.source()) != EMPTYVALUE && !fun1.test(t))
				;
			return t;
		};
	}

	public static FltSource_ flatten(Source<Iterable<Float>> source) {
		return new FltSource_() {
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

	public static <R> R fold(Fun<FltObjPair<R>, R> fun0, R init, FltSource_ source) {
		Fun<FltObjPair<R>, R> fun1 = Rethrow.fun(fun0);
		float t;
		while ((t = source.source()) != EMPTYVALUE)
			init = fun1.apply(FltObjPair.of(t, init));
		return init;
	}

	public static boolean isAll(FltPredicate_ pred0, FltSource_ source) {
		FltPredicate_ pred1 = pred0.rethrow();
		float t;
		while ((t = source.source()) != EMPTYVALUE)
			if (!pred1.test(t))
				return false;
		return true;
	}

	public static boolean isAny(FltPredicate_ pred0, FltSource_ source) {
		FltPredicate_ pred1 = pred0.rethrow();
		float t;
		while ((t = source.source()) != EMPTYVALUE)
			if (pred1.test(t))
				return true;
		return false;
	}

	public static Iterator<Float> iterator(FltSource_ source) {
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

	public static Iterable<Float> iter(FltSource_ source) {
		return () -> iterator(source);
	}

	public static <T1> Source<T1> map(Flt_Obj<T1> fun0, FltSource_ source) {
		Flt_Obj<T1> fun1 = fun0.rethrow();
		return () -> {
			float t0 = source.source();
			return t0 != FltFunUtil.EMPTYVALUE ? fun1.apply(t0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Flt_Obj<K> kf0, Flt_Obj<V> vf0, FltSource_ source) {
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

	public static FltSource_ mapFlt(Flt_Flt fun0, FltSource_ source) {
		Flt_Flt fun1 = fun0.rethrow();
		return () -> {
			float t = source.source();
			return t != FltFunUtil.EMPTYVALUE ? fun1.apply(t) : FltFunUtil.EMPTYVALUE;
		};
	}

	public static <T0, T1> Source<T1> mapNonNull(Fun<T0, T1> fun0, Source<T0> source) {
		Fun<T0, T1> fun1 = Rethrow.fun(fun0);
		return new Source<T1>() {
			public T1 source() {
				T0 t0;
				T1 t1;
				while ((t0 = source.source()) != null)
					if ((t1 = fun1.apply(t0)) != null)
						return t1;
				return null;
			}
		};
	}

	public static FltSink_ nullSink() {
		return i -> {
		};
	}

	public static FltSource_ nullSource() {
		return () -> EMPTYVALUE;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<FltSource_> split(FltPredicate_ fun0, FltSource_ source) {
		FltPredicate_ fun1 = fun0.rethrow();
		return new Source<FltSource_>() {
			private float t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private FltSource_ source_ = () -> (isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && !fun1.test(t) ? t
					: null;

			public FltSource_ source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static FltSource_ suck(Sink<FltSink_> fun) {
		NullableSyncQueue<Float> queue = new NullableSyncQueue<>();
		FltSink_ enqueue = t -> enqueue(queue, t);

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
