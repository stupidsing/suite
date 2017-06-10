package suite.primitive;

import java.util.Collections;
import java.util.Iterator;

import suite.adt.pair.DblObjPair;
import suite.os.LogUtil;
import suite.primitive.DblPrimitives.DblPredicate;
import suite.primitive.DblPrimitives.DblSink;
import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.DblPrimitives.Dbl_Obj;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class DblFunUtil {

	public static double EMPTYVALUE = Double.MIN_VALUE;

	public static DblSource append(double t, DblSource source) {
		return new DblSource() {
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

	public static Source<DblSource> chunk(int n, DblSource source) {
		return new Source<DblSource>() {
			private double t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private int i;
			private DblSource source_ = () -> {
				if ((isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && ++i < n)
					return t;
				else {
					i = 0;
					return EMPTYVALUE;
				}
			};

			public DblSource source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	public static DblSource concat(Source<DblSource> source) {
		return new DblSource() {
			private DblSource source0 = nullSource();

			public double source() {
				double t = EMPTYVALUE;
				while (source0 != null && (t = source0.source()) == EMPTYVALUE)
					source0 = source.source();
				return t;
			}
		};
	}

	public static DblSource cons(double t, DblSource source) {
		return new DblSource() {
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

	public static DblSource filter(DblPredicate fun0, DblSource source) {
		DblPredicate fun1 = fun0.rethrow();
		return () -> {
			double t = EMPTYVALUE;
			while ((t = source.source()) != EMPTYVALUE && !fun1.test(t))
				;
			return t;
		};
	}

	public static DblSource flatten(Source<Iterable<Double>> source) {
		return new DblSource() {
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

	public static <R> R fold(Fun<DblObjPair<R>, R> fun0, R init, DblSource source) {
		Fun<DblObjPair<R>, R> fun1 = Rethrow.fun(fun0);
		double t;
		while ((t = source.source()) != EMPTYVALUE)
			init = fun1.apply(DblObjPair.of(t, init));
		return init;
	}

	public static boolean isAll(DblPredicate pred0, DblSource source) {
		DblPredicate pred1 = pred0.rethrow();
		double t;
		while ((t = source.source()) != EMPTYVALUE)
			if (!pred1.test(t))
				return false;
		return true;
	}

	public static boolean isAny(DblPredicate pred0, DblSource source) {
		DblPredicate pred1 = pred0.rethrow();
		double t;
		while ((t = source.source()) != EMPTYVALUE)
			if (pred1.test(t))
				return true;
		return false;
	}

	public static Iterator<Double> iterator(DblSource source) {
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

	public static Iterable<Double> iter(DblSource source) {
		return () -> iterator(source);
	}

	public static <T1> Source<T1> map(Dbl_Obj<T1> fun0, DblSource source) {
		Dbl_Obj<T1> fun1 = fun0.rethrow();
		return () -> {
			double t0 = source.source();
			return t0 != DblFunUtil.EMPTYVALUE ? fun1.apply(t0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Dbl_Obj<K> kf0, Dbl_Obj<V> vf0, DblSource source) {
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

	public static DblSource mapDbl(Dbl_Dbl fun0, DblSource source) {
		Dbl_Dbl fun1 = fun0.rethrow();
		return () -> {
			double t = source.source();
			return t != DblFunUtil.EMPTYVALUE ? fun1.apply(t) : DblFunUtil.EMPTYVALUE;
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

	public static DblSink nullSink() {
		return i -> {
		};
	}

	public static DblSource nullSource() {
		return () -> EMPTYVALUE;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<DblSource> split(DblPredicate fun0, DblSource source) {
		DblPredicate fun1 = fun0.rethrow();
		return new Source<DblSource>() {
			private double t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private DblSource source_ = () -> (isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && !fun1.test(t) ? t
					: null;

			public DblSource source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static DblSource suck(Sink<DblSink> fun) {
		NullableSyncQueue<Double> queue = new NullableSyncQueue<>();
		DblSink enqueue = t -> enqueue(queue, t);

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
