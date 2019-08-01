package suite.primitive;

import static primal.statics.Fail.fail;

import java.util.Collections;
import java.util.Iterator;

import primal.NullableSyncQueue;
import primal.Verbs.Start;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Source2;
import primal.os.Log_;
import primal.primitive.DblPrim;
import primal.primitive.DblPrim.DblObjSource;
import primal.primitive.DblPrim.DblSink;
import primal.primitive.DblPrim.DblSource;
import primal.primitive.DblPrim.DblTest;
import primal.primitive.adt.pair.DblObjPair;
import primal.statics.Fail.InterruptedRuntimeException;
import suite.primitive.DblPrimitives.Dbl_Obj;

public class DblFunUtil {

	public static Source<DblSource> chunk(int n, DblSource source) {
		return new Source<>() {
			private double c = source.g();
			private boolean isAvail = c != DblPrim.EMPTYVALUE;
			private int i;
			private DblSource source_ = () -> {
				if ((isAvail = isAvail && (c = source.g()) != DblPrim.EMPTYVALUE) && ++i < n)
					return c;
				else {
					i = 0;
					return DblPrim.EMPTYVALUE;
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
				var c = DblPrim.EMPTYVALUE;
				while (source0 != null && (c = source0.g()) == DblPrim.EMPTYVALUE)
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
			var c = DblPrim.EMPTYVALUE;
			while ((c = source.g()) != DblPrim.EMPTYVALUE && !fun1.test(c))
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
						return DblPrim.EMPTYVALUE;
				return iter.next();
			}
		};
	}

	public static <R> R fold(Fun<DblObjPair<R>, R> fun0, R init, DblSource source) {
		var fun1 = fun0.rethrow();
		double c;
		while ((c = source.g()) != DblPrim.EMPTYVALUE)
			init = fun1.apply(DblObjPair.of(c, init));
		return init;
	}

	public static boolean isAll(DblTest pred0, DblSource source) {
		var pred1 = pred0.rethrow();
		double c;
		while ((c = source.g()) != DblPrim.EMPTYVALUE)
			if (!pred1.test(c))
				return false;
		return true;
	}

	public static boolean isAny(DblTest pred0, DblSource source) {
		var pred1 = pred0.rethrow();
		double c;
		while ((c = source.g()) != DblPrim.EMPTYVALUE)
			if (pred1.test(c))
				return true;
		return false;
	}

	public static Iterator<Double> iterator(DblSource source) {
		return new Iterator<>() {
			private double next = DblPrim.EMPTYVALUE;

			public boolean hasNext() {
				if (next == DblPrim.EMPTYVALUE)
					next = source.g();
				return next != DblPrim.EMPTYVALUE;
			}

			public Double next() {
				var next0 = next;
				next = DblPrim.EMPTYVALUE;
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
			return c0 != DblPrim.EMPTYVALUE ? fun1.apply(c0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Dbl_Obj<K> kf0, Dbl_Obj<V> vf0, DblSource source) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		return pair -> {
			var c = source.g();
			var b = c != DblPrim.EMPTYVALUE;
			if (b)
				pair.update(kf1.apply(c), vf1.apply(c));
			return b;
		};
	}

	public static DblSource mapDbl(Dbl_Dbl fun0, DblSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c = source.g();
			return c != DblPrim.EMPTYVALUE ? fun1.apply(c) : DblPrim.EMPTYVALUE;
		};
	}

	public static <V> DblObjSource<V> mapDblObj(Dbl_Obj<V> fun0, DblSource source) {
		var fun1 = fun0.rethrow();
		return pair -> {
			var c = source.g();
			if (c != DblPrim.EMPTYVALUE) {
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
		return () -> DblPrim.EMPTYVALUE;
	}

	public static DblSource snoc(double c, DblSource source) {
		return new DblSource() {
			private boolean isAppended = false;

			public double g() {
				if (!isAppended) {
					var c_ = source.g();
					if (c_ != DblPrim.EMPTYVALUE)
						return c_;
					else {
						isAppended = true;
						return c;
					}
				} else
					return DblPrim.EMPTYVALUE;
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
			private boolean isAvail = c != DblPrim.EMPTYVALUE;
			private DblSource source_ = () -> (isAvail = isAvail && (c = source.g()) != DblPrim.EMPTYVALUE) && !fun1.test(c) ? c : null;

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

		var thread = Start.thread(() -> {
			try {
				fun.f(enqueue);
			} finally {
				enqueue(queue, DblPrim.EMPTYVALUE);
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
