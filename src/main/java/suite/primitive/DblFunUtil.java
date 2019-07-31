package suite.primitive;

import static primal.statics.Fail.fail;

import java.util.Collections;
import java.util.Iterator;

import primal.os.Log_;
import primal.statics.Fail.InterruptedRuntimeException;
import suite.primitive.DblPrimitives.DblObjSource;
import suite.primitive.DblPrimitives.DblSink;
import suite.primitive.DblPrimitives.DblSource;
import suite.primitive.DblPrimitives.DblTest;
import suite.primitive.DblPrimitives.Dbl_Obj;
import suite.primitive.adt.pair.DblObjPair;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Thread_;

public class DblFunUtil {

	public static double EMPTYVALUE = Double.MIN_VALUE;

	public static Source<DblSource> chunk(int n, DblSource source) {
		return new Source<>() {
			private double c = source.g();
			private boolean isAvail = c != EMPTYVALUE;
			private int i;
			private DblSource source_ = () -> {
				if ((isAvail = isAvail && (c = source.g()) != EMPTYVALUE) && ++i < n)
					return c;
				else {
					i = 0;
					return EMPTYVALUE;
				}
			};

			public DblSource g() {
				return isAvail ? cons(c, source_) : null;
			}
		};
	}

	public static DblSource concat(Source<DblSource> source) {
		return new DblSource() {
			private DblSource source0 = nullSource();

			public double g() {
				var c = EMPTYVALUE;
				while (source0 != null && (c = source0.g()) == EMPTYVALUE)
					source0 = source.g();
				return c;
			}
		};
	}

	public static DblSource cons(double c, DblSource source) {
		return new DblSource() {
			private boolean isFirst = true;

			public double g() {
				if (!isFirst)
					return source.g();
				else {
					isFirst = false;
					return c;
				}
			}
		};
	}

	public static DblSource filter(DblTest fun0, DblSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c = EMPTYVALUE;
			while ((c = source.g()) != EMPTYVALUE && !fun1.test(c))
				;
			return c;
		};
	}

	public static DblSource flatten(Source<Iterable<Double>> source) {
		return new DblSource() {
			private Iterator<Double> iter = Collections.emptyIterator();

			public double g() {
				Iterable<Double> iterable;
				while (!iter.hasNext())
					if ((iterable = source.g()) != null)
						iter = iterable.iterator();
					else
						return EMPTYVALUE;
				return iter.next();
			}
		};
	}

	public static <R> R fold(Fun<DblObjPair<R>, R> fun0, R init, DblSource source) {
		var fun1 = fun0.rethrow();
		double c;
		while ((c = source.g()) != EMPTYVALUE)
			init = fun1.apply(DblObjPair.of(c, init));
		return init;
	}

	public static boolean isAll(DblTest pred0, DblSource source) {
		var pred1 = pred0.rethrow();
		double c;
		while ((c = source.g()) != EMPTYVALUE)
			if (!pred1.test(c))
				return false;
		return true;
	}

	public static boolean isAny(DblTest pred0, DblSource source) {
		var pred1 = pred0.rethrow();
		double c;
		while ((c = source.g()) != EMPTYVALUE)
			if (pred1.test(c))
				return true;
		return false;
	}

	public static Iterator<Double> iterator(DblSource source) {
		return new Iterator<>() {
			private double next = EMPTYVALUE;

			public boolean hasNext() {
				if (next == EMPTYVALUE)
					next = source.g();
				return next != EMPTYVALUE;
			}

			public Double next() {
				var next0 = next;
				next = EMPTYVALUE;
				return next0;
			}

		};
	}

	public static Iterable<Double> iter(DblSource source) {
		return () -> iterator(source);
	}

	public static <T1> Source<T1> map(Dbl_Obj<T1> fun0, DblSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c0 = source.g();
			return c0 != DblFunUtil.EMPTYVALUE ? fun1.apply(c0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Dbl_Obj<K> kf0, Dbl_Obj<V> vf0, DblSource source) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		return pair -> {
			var c = source.g();
			var b = c != EMPTYVALUE;
			if (b)
				pair.update(kf1.apply(c), vf1.apply(c));
			return b;
		};
	}

	public static DblSource mapDbl(Dbl_Dbl fun0, DblSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c = source.g();
			return c != DblFunUtil.EMPTYVALUE ? fun1.apply(c) : DblFunUtil.EMPTYVALUE;
		};
	}

	public static <V> DblObjSource<V> mapDblObj(Dbl_Obj<V> fun0, DblSource source) {
		var fun1 = fun0.rethrow();
		return pair -> {
			var c = source.g();
			if (c != DblFunUtil.EMPTYVALUE) {
				pair.update(c, fun1.apply(c));
				return true;
			} else
				return false;

		};
	}

	public static DblSink nullSink() {
		return i -> {
		};
	}

	public static DblSource nullSource() {
		return () -> EMPTYVALUE;
	}

	public static DblSource snoc(double c, DblSource source) {
		return new DblSource() {
			private boolean isAppended = false;

			public double g() {
				if (!isAppended) {
					var c_ = source.g();
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

	/**
	 * Problematic split: all data must be read, i.e. the children lists must not be
	 * skipped.
	 */
	public static Source<DblSource> split(DblTest fun0, DblSource source) {
		var fun1 = fun0.rethrow();
		return new Source<>() {
			private double c = source.g();
			private boolean isAvail = c != EMPTYVALUE;
			private DblSource source_ = () -> (isAvail = isAvail && (c = source.g()) != EMPTYVALUE) && !fun1.test(c) ? c : null;

			public DblSource g() {
				return isAvail ? cons(c, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and make it into a source.
	 */
	public static DblSource suck(Sink<DblSink> fun) {
		var queue = new NullableSyncQueue<Double>();
		DblSink enqueue = c -> enqueue(queue, c);

		var thread = Thread_.startThread(() -> {
			try {
				fun.f(enqueue);
			} finally {
				enqueue(queue, EMPTYVALUE);
			}
		});

		return () -> {
			try {
				return queue.take();
			} catch (InterruptedException | InterruptedRuntimeException ex) {
				thread.interrupt();
				return fail(ex);
			}
		};
	}

	private static void enqueue(NullableSyncQueue<Double> queue, double c) {
		try {
			queue.offer(c);
		} catch (InterruptedException ex) {
			Log_.error(ex);
		}
	}

}
