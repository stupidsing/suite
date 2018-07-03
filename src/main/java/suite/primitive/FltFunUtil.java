package suite.primitive;

import java.util.Collections;
import java.util.Iterator;

import suite.os.LogUtil;
import suite.primitive.FltPrimitives.FltObjSource;
import suite.primitive.FltPrimitives.FltSink;
import suite.primitive.FltPrimitives.FltSource;
import suite.primitive.FltPrimitives.FltTest;
import suite.primitive.FltPrimitives.Flt_Obj;
import suite.primitive.adt.pair.FltObjPair;
import suite.util.Fail;
import suite.util.Fail.InterruptedRuntimeException;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Thread_;

public class FltFunUtil {

	public static float EMPTYVALUE = Float.MIN_VALUE;

	public static Source<FltSource> chunk(int n, FltSource source) {
		return new Source<>() {
			private float c = source.source();
			private boolean isAvail = c != EMPTYVALUE;
			private int i;
			private FltSource source_ = () -> {
				if ((isAvail = isAvail && (c = source.source()) != EMPTYVALUE) && ++i < n)
					return c;
				else {
					i = 0;
					return EMPTYVALUE;
				}
			};

			public FltSource source() {
				return isAvail ? cons(c, source_) : null;
			}
		};
	}

	public static FltSource concat(Source<FltSource> source) {
		return new FltSource() {
			private FltSource source0 = nullSource();

			public float source() {
				var c = EMPTYVALUE;
				while (source0 != null && (c = source0.source()) == EMPTYVALUE)
					source0 = source.source();
				return c;
			}
		};
	}

	public static FltSource cons(float c, FltSource source) {
		return new FltSource() {
			private boolean isFirst = true;

			public float source() {
				if (!isFirst)
					return source.source();
				else {
					isFirst = false;
					return c;
				}
			}
		};
	}

	public static FltSource filter(FltTest fun0, FltSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c = EMPTYVALUE;
			while ((c = source.source()) != EMPTYVALUE && !fun1.test(c))
				;
			return c;
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
		var fun1 = fun0.rethrow();
		float c;
		while ((c = source.source()) != EMPTYVALUE)
			init = fun1.apply(FltObjPair.of(c, init));
		return init;
	}

	public static boolean isAll(FltTest pred0, FltSource source) {
		var pred1 = pred0.rethrow();
		float c;
		while ((c = source.source()) != EMPTYVALUE)
			if (!pred1.test(c))
				return false;
		return true;
	}

	public static boolean isAny(FltTest pred0, FltSource source) {
		var pred1 = pred0.rethrow();
		float c;
		while ((c = source.source()) != EMPTYVALUE)
			if (pred1.test(c))
				return true;
		return false;
	}

	public static Iterator<Float> iterator(FltSource source) {
		return new Iterator<>() {
			private float next = EMPTYVALUE;

			public boolean hasNext() {
				if (next == EMPTYVALUE)
					next = source.source();
				return next != EMPTYVALUE;
			}

			public Float next() {
				var next0 = next;
				next = EMPTYVALUE;
				return next0;
			}

		};
	}

	public static Iterable<Float> iter(FltSource source) {
		return () -> iterator(source);
	}

	public static <T1> Source<T1> map(Flt_Obj<T1> fun0, FltSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c0 = source.source();
			return c0 != FltFunUtil.EMPTYVALUE ? fun1.apply(c0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Flt_Obj<K> kf0, Flt_Obj<V> vf0, FltSource source) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		return pair -> {
			var c = source.source();
			var b = c != EMPTYVALUE;
			if (b)
				pair.update(kf1.apply(c), vf1.apply(c));
			return b;
		};
	}

	public static FltSource mapFlt(Flt_Flt fun0, FltSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c = source.source();
			return c != FltFunUtil.EMPTYVALUE ? fun1.apply(c) : FltFunUtil.EMPTYVALUE;
		};
	}

	public static <V> FltObjSource<V> mapFltObj(Flt_Obj<V> fun0, FltSource source) {
		var fun1 = fun0.rethrow();
		return pair -> {
			var c = source.source();
			if (c != FltFunUtil.EMPTYVALUE) {
				pair.update(c, fun1.apply(c));
				return true;
			} else
				return false;

		};
	}

	public static FltSink nullSink() {
		return i -> {
		};
	}

	public static FltSource nullSource() {
		return () -> EMPTYVALUE;
	}

	public static FltSource snoc(float c, FltSource source) {
		return new FltSource() {
			private boolean isAppended = false;

			public float source() {
				if (!isAppended) {
					var c_ = source.source();
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
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<FltSource> split(FltTest fun0, FltSource source) {
		var fun1 = fun0.rethrow();
		return new Source<>() {
			private float c = source.source();
			private boolean isAvail = c != EMPTYVALUE;
			private FltSource source_ = () -> (isAvail = isAvail && (c = source.source()) != EMPTYVALUE) && !fun1.test(c) ? c
					: null;

			public FltSource source() {
				return isAvail ? cons(c, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and make it into a source.
	 */
	public static FltSource suck(Sink<FltSink> fun) {
		var queue = new NullableSyncQueue<Float>();
		FltSink enqueue = c -> enqueue(queue, c);

		var thread = Thread_.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, EMPTYVALUE);
			}
		});

		return () -> {
			try {
				return queue.take();
			} catch (InterruptedException | InterruptedRuntimeException ex) {
				thread.interrupt();
				return Fail.t(ex);
			}
		};
	}

	private static void enqueue(NullableSyncQueue<Float> queue, float c) {
		try {
			queue.offer(c);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
		}
	}

}
