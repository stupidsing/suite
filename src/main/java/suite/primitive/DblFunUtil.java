package suite.primitive;

import java.util.Collections;
import java.util.Iterator;

import suite.adt.pair.DblObjPair;
import suite.os.LogUtil;
import suite.primitive.DblFun.Dbl_Obj;
import suite.primitive.DblPredicate.DblPredicate_;
import suite.primitive.DblSink.DblSink_;
import suite.primitive.DblSource.DblSource_;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class DblFunUtil {

	public static double EMPTYVALUE = Double.MIN_VALUE;

	public static DblSource_ append(double t, DblSource_ source) {
		return new DblSource_() {
			private boolean isAppended = false;

			public double source() {
				if (!isAppended) {
					double t_ = source.source();
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

	public static Source<DblSource_> chunk(int n, DblSource_ source) {
		return new Source<DblSource_>() {
			private double t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private int i;
			private DblSource_ source_ = () -> {
				if ((isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && ++i < n)
					return t;
				else {
					i = 0;
					return EMPTYVALUE;
				}
			};

			public DblSource_ source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	public static DblSource_ concat(Source<DblSource_> source) {
		return new DblSource_() {
			private DblSource_ source0 = nullSource();

			public double source() {
				double t = EMPTYVALUE;
				while (source0 != null && (t = source0.source()) == EMPTYVALUE)
					source0 = source.source();
				return t;
			}
		};
	}

	public static DblSource_ cons(double t, DblSource_ source) {
		return new DblSource_() {
			private boolean isFirst = true;

			public double source() {
				if (!isFirst)
					return source.source();
				else {
					isFirst = false;
					return t;
				}
			}
		};
	}

	public static DblSource_ filter(DblPredicate_ fun0, DblSource_ source) {
		DblPredicate_ fun1 = fun0.rethrow();
		return () -> {
			double t = EMPTYVALUE;
			while ((t = source.source()) != EMPTYVALUE && !fun1.test(t))
				;
			return t;
		};
	}

	public static DblSource_ flatten(Source<Iterable<Double>> source) {
		return new DblSource_() {
			private Iterator<Double> iter = Collections.emptyIterator();

			public double source() {
				Iterable<Double> iterable;
				while (!iter.hasNext())
					if ((iterable = source.source()) != null)
						iter = iterable.iterator();
					else
						return EMPTYVALUE;
				return iter.next();
			}
		};
	}

	public static <R> R fold(Fun<DblObjPair<R>, R> fun0, R init, DblSource_ source) {
		Fun<DblObjPair<R>, R> fun1 = Rethrow.fun(fun0);
		double t;
		while ((t = source.source()) != EMPTYVALUE)
			init = fun1.apply(DblObjPair.of(t, init));
		return init;
	}

	public static boolean isAll(DblPredicate_ pred0, DblSource_ source) {
		DblPredicate_ pred1 = pred0.rethrow();
		double t;
		while ((t = source.source()) != EMPTYVALUE)
			if (!pred1.test(t))
				return false;
		return true;
	}

	public static boolean isAny(DblPredicate_ pred0, DblSource_ source) {
		DblPredicate_ pred1 = pred0.rethrow();
		double t;
		while ((t = source.source()) != EMPTYVALUE)
			if (pred1.test(t))
				return true;
		return false;
	}

	public static Iterator<Double> iterator(DblSource_ source) {
		return new Iterator<Double>() {
			private double next = EMPTYVALUE;

			public boolean hasNext() {
				if (next == EMPTYVALUE)
					next = source.source();
				return next != EMPTYVALUE;
			}

			public Double next() {
				double next0 = next;
				next = EMPTYVALUE;
				return next0;
			}

		};
	}

	public static Iterable<Double> iter(DblSource_ source) {
		return () -> iterator(source);
	}

	public static <T0, T1> Source<T1> map(Fun<T0, T1> fun0, Source<T0> source) {
		Fun<T0, T1> fun1 = Rethrow.fun(fun0);
		return () -> {
			T0 t0 = source.source();
			return t0 != null ? fun1.apply(t0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Dbl_Obj<K> kf0, Dbl_Obj<V> vf0, DblSource_ source) {
		Dbl_Obj<K> kf1 = kf0.rethrow();
		Dbl_Obj<V> vf1 = vf0.rethrow();
		return pair -> {
			double t = source.source();
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

	public static DblSink_ nullSink() {
		return i -> {
		};
	}

	public static DblSource_ nullSource() {
		return () -> EMPTYVALUE;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<DblSource_> split(DblPredicate_ fun0, DblSource_ source) {
		DblPredicate_ fun1 = fun0.rethrow();
		return new Source<DblSource_>() {
			private double t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private DblSource_ source_ = () -> (isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && !fun1.test(t) ? t
					: null;

			public DblSource_ source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static DblSource_ suck(Sink<DblSink_> fun) {
		NullableSyncQueue<Double> queue = new NullableSyncQueue<>();
		DblSink_ enqueue = t -> enqueue(queue, t);

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

	private static void enqueue(NullableSyncQueue<Double> queue, double t) {
		try {
			queue.offer(t);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
		}
	}

}
