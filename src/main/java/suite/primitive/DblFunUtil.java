package suite.primitive;

import java.util.Collections;
import java.util.Iterator;

import suite.os.LogUtil;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.DblPredicate;
import suite.primitive.DblPrimitives.DblSink;
import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.DblPrimitives.Dbl_Obj;
import suite.primitive.adt.pair.DblObjPair;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Thread_;

public class DblFunUtil {

	public static double EMPTYVALUE = Double.MIN_VALUE;

	public static DblSource append(double c, DblSource source) {
		return new DblSource() {
			private boolean isAppended = false;

			public double source() {
				if (!isAppended) {
					double c_ = source.source();
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

	public static Source<DblSource> chunk(int n, DblSource source) {
		return new Source<DblSource>() {
			private double c = source.source();
			private boolean isAvail = c != EMPTYVALUE;
			private int i;
			private DblSource source_ = () -> {
				if ((isAvail = isAvail && (c = source.source()) != EMPTYVALUE) && ++i < n)
					return c;
				else {
					i = 0;
					return EMPTYVALUE;
				}
			};

			public DblSource source() {
				return isAvail ? cons(c, source_) : null;
			}
		};
	}

	public static DblSource concat(Source<DblSource> source) {
		return new DblSource() {
			private DblSource source0 = nullSource();

			public double source() {
				double c = EMPTYVALUE;
				while (source0 != null && (c = source0.source()) == EMPTYVALUE)
					source0 = source.source();
				return c;
			}
		};
	}

	public static DblSource cons(double c, DblSource source) {
		return new DblSource() {
			private boolean isFirst = true;

			public double source() {
				if (!isFirst)
					return source.source();
				else {
					isFirst = false;
					return c;
				}
			}
		};
	}

	public static DblSource filter(DblPredicate fun0, DblSource source) {
		DblPredicate fun1 = fun0.rethrow();
		return () -> {
			double c = EMPTYVALUE;
			while ((c = source.source()) != EMPTYVALUE && !fun1.test(c))
				;
			return c;
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
		Fun<DblObjPair<R>, R> fun1 = fun0.rethrow();
		double c;
		while ((c = source.source()) != EMPTYVALUE)
			init = fun1.apply(DblObjPair.of(c, init));
		return init;
	}

	public static boolean isAll(DblPredicate pred0, DblSource source) {
		DblPredicate pred1 = pred0.rethrow();
		double c;
		while ((c = source.source()) != EMPTYVALUE)
			if (!pred1.test(c))
				return false;
		return true;
	}

	public static boolean isAny(DblPredicate pred0, DblSource source) {
		DblPredicate pred1 = pred0.rethrow();
		double c;
		while ((c = source.source()) != EMPTYVALUE)
			if (pred1.test(c))
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
			double c0 = source.source();
			return c0 != DblFunUtil.EMPTYVALUE ? fun1.apply(c0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Dbl_Obj<K> kf0, Dbl_Obj<V> vf0, DblSource source) {
		Dbl_Obj<K> kf1 = kf0.rethrow();
		Dbl_Obj<V> vf1 = vf0.rethrow();
		return pair -> {
			double c = source.source();
			boolean b = c != EMPTYVALUE;
			if (b) {
				pair.t0 = kf1.apply(c);
				pair.t1 = vf1.apply(c);
			}
			return b;
		};
	}

	public static DblSource mapDbl(Dbl_Dbl fun0, DblSource source) {
		Dbl_Dbl fun1 = fun0.rethrow();
		return () -> {
			double c = source.source();
			return c != DblFunUtil.EMPTYVALUE ? fun1.apply(c) : DblFunUtil.EMPTYVALUE;
		};
	}

	public static <V> DblObjSource<V> mapDblObj(Dbl_Obj<V> fun0, DblSource source) {
		Dbl_Obj<V> fun1 = fun0.rethrow();
		return pair -> {
			double c = source.source();
			if (c != DblFunUtil.EMPTYVALUE) {
				pair.t0 = c;
				pair.t1 = fun1.apply(c);
				return true;
			} else
				return false;

		};
	}

	public static <T> Source<T> mapNonNull(Dbl_Obj<T> fun0, DblSource source) {
		Dbl_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				double c;
				T t;
				while ((c = source.source()) != DblFunUtil.EMPTYVALUE)
					if ((t = fun1.apply(c)) != null)
						return t;
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
	 * Problematic split: all data must be read, i.e. the children lists must not be
	 * skipped.
	 */
	public static Source<DblSource> split(DblPredicate fun0, DblSource source) {
		DblPredicate fun1 = fun0.rethrow();
		return new Source<DblSource>() {
			private double c = source.source();
			private boolean isAvail = c != EMPTYVALUE;
			private DblSource source_ = () -> (isAvail = isAvail && (c = source.source()) != EMPTYVALUE) && !fun1.test(c) ? c
					: null;

			public DblSource source() {
				return isAvail ? cons(c, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static DblSource suck(Sink<DblSink> fun) {
		NullableSyncQueue<Double> queue = new NullableSyncQueue<>();
		DblSink enqueue = c -> enqueue(queue, c);

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

	private static void enqueue(NullableSyncQueue<Double> queue, double c) {
		try {
			queue.offer(c);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
		}
	}

}
