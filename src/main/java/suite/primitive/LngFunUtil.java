package suite.primitive;

import static suite.util.Friends.fail;

import java.util.Collections;
import java.util.Iterator;

import suite.os.Log_;
import suite.primitive.LngPrimitives.LngObjSource;
import suite.primitive.LngPrimitives.LngSink;
import suite.primitive.LngPrimitives.LngSource;
import suite.primitive.LngPrimitives.LngTest;
import suite.primitive.LngPrimitives.Lng_Obj;
import suite.primitive.adt.pair.LngObjPair;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Source2;
import suite.util.Fail.InterruptedRuntimeException;
import suite.util.NullableSyncQueue;
import suite.util.Thread_;

public class LngFunUtil {

	public static long EMPTYVALUE = Long.MIN_VALUE;

	public static Source<LngSource> chunk(int n, LngSource source) {
		return new Source<>() {
			private long c = source.g();
			private boolean isAvail = c != EMPTYVALUE;
			private int i;
			private LngSource source_ = () -> {
				if ((isAvail = isAvail && (c = source.g()) != EMPTYVALUE) && ++i < n)
					return c;
				else {
					i = 0;
					return EMPTYVALUE;
				}
			};

			public LngSource g() {
				return isAvail ? cons(c, source_) : null;
			}
		};
	}

	public static LngSource concat(Source<LngSource> source) {
		return new LngSource() {
			private LngSource source0 = nullSource();

			public long g() {
				var c = EMPTYVALUE;
				while (source0 != null && (c = source0.g()) == EMPTYVALUE)
					source0 = source.g();
				return c;
			}
		};
	}

	public static LngSource cons(long c, LngSource source) {
		return new LngSource() {
			private boolean isFirst = true;

			public long g() {
				if (!isFirst)
					return source.g();
				else {
					isFirst = false;
					return c;
				}
			}
		};
	}

	public static LngSource filter(LngTest fun0, LngSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c = EMPTYVALUE;
			while ((c = source.g()) != EMPTYVALUE && !fun1.test(c))
				;
			return c;
		};
	}

	public static LngSource flatten(Source<Iterable<Long>> source) {
		return new LngSource() {
			private Iterator<Long> iter = Collections.emptyIterator();

			public long g() {
				Iterable<Long> iterable;
				while (!iter.hasNext())
					if ((iterable = source.g()) != null)
						iter = iterable.iterator();
					else
						return EMPTYVALUE;
				return iter.next();
			}
		};
	}

	public static <R> R fold(Fun<LngObjPair<R>, R> fun0, R init, LngSource source) {
		var fun1 = fun0.rethrow();
		long c;
		while ((c = source.g()) != EMPTYVALUE)
			init = fun1.apply(LngObjPair.of(c, init));
		return init;
	}

	public static boolean isAll(LngTest pred0, LngSource source) {
		var pred1 = pred0.rethrow();
		long c;
		while ((c = source.g()) != EMPTYVALUE)
			if (!pred1.test(c))
				return false;
		return true;
	}

	public static boolean isAny(LngTest pred0, LngSource source) {
		var pred1 = pred0.rethrow();
		long c;
		while ((c = source.g()) != EMPTYVALUE)
			if (pred1.test(c))
				return true;
		return false;
	}

	public static Iterator<Long> iterator(LngSource source) {
		return new Iterator<>() {
			private long next = EMPTYVALUE;

			public boolean hasNext() {
				if (next == EMPTYVALUE)
					next = source.g();
				return next != EMPTYVALUE;
			}

			public Long next() {
				var next0 = next;
				next = EMPTYVALUE;
				return next0;
			}

		};
	}

	public static Iterable<Long> iter(LngSource source) {
		return () -> iterator(source);
	}

	public static <T1> Source<T1> map(Lng_Obj<T1> fun0, LngSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c0 = source.g();
			return c0 != LngFunUtil.EMPTYVALUE ? fun1.apply(c0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Lng_Obj<K> kf0, Lng_Obj<V> vf0, LngSource source) {
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

	public static LngSource mapLng(Lng_Lng fun0, LngSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c = source.g();
			return c != LngFunUtil.EMPTYVALUE ? fun1.apply(c) : LngFunUtil.EMPTYVALUE;
		};
	}

	public static <V> LngObjSource<V> mapLngObj(Lng_Obj<V> fun0, LngSource source) {
		var fun1 = fun0.rethrow();
		return pair -> {
			var c = source.g();
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

	public static LngSource snoc(long c, LngSource source) {
		return new LngSource() {
			private boolean isAppended = false;

			public long g() {
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
	public static Source<LngSource> split(LngTest fun0, LngSource source) {
		var fun1 = fun0.rethrow();
		return new Source<>() {
			private long c = source.g();
			private boolean isAvail = c != EMPTYVALUE;
			private LngSource source_ = () -> (isAvail = isAvail && (c = source.g()) != EMPTYVALUE) && !fun1.test(c) ? c : null;

			public LngSource g() {
				return isAvail ? cons(c, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and make it into a source.
	 */
	public static LngSource suck(Sink<LngSink> fun) {
		var queue = new NullableSyncQueue<Long>();
		LngSink enqueue = c -> enqueue(queue, c);

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

	private static void enqueue(NullableSyncQueue<Long> queue, long c) {
		try {
			queue.offer(c);
		} catch (InterruptedException ex) {
			Log_.error(ex);
		}
	}

}
