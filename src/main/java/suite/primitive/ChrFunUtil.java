package suite.primitive;

import java.util.Collections;
import java.util.Iterator;

import suite.os.LogUtil;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.ChrSink;
import suite.primitive.ChrPrimitives.ChrSource;
import suite.primitive.ChrPrimitives.ChrTest;
import suite.primitive.ChrPrimitives.Chr_Obj;
import suite.primitive.adt.pair.ChrObjPair;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Source2;
import suite.util.Fail;
import suite.util.Fail.InterruptedRuntimeException;
import suite.util.NullableSyncQueue;
import suite.util.Thread_;

public class ChrFunUtil {

	public static char EMPTYVALUE = Character.MIN_VALUE;

	public static Source<ChrSource> chunk(int n, ChrSource source) {
		return new Source<>() {
			private char c = source.source();
			private boolean isAvail = c != EMPTYVALUE;
			private int i;
			private ChrSource source_ = () -> {
				if ((isAvail = isAvail && (c = source.source()) != EMPTYVALUE) && ++i < n)
					return c;
				else {
					i = 0;
					return EMPTYVALUE;
				}
			};

			public ChrSource source() {
				return isAvail ? cons(c, source_) : null;
			}
		};
	}

	public static ChrSource concat(Source<ChrSource> source) {
		return new ChrSource() {
			private ChrSource source0 = nullSource();

			public char source() {
				var c = EMPTYVALUE;
				while (source0 != null && (c = source0.source()) == EMPTYVALUE)
					source0 = source.source();
				return c;
			}
		};
	}

	public static ChrSource cons(char c, ChrSource source) {
		return new ChrSource() {
			private boolean isFirst = true;

			public char source() {
				if (!isFirst)
					return source.source();
				else {
					isFirst = false;
					return c;
				}
			}
		};
	}

	public static ChrSource filter(ChrTest fun0, ChrSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c = EMPTYVALUE;
			while ((c = source.source()) != EMPTYVALUE && !fun1.test(c))
				;
			return c;
		};
	}

	public static ChrSource flatten(Source<Iterable<Character>> source) {
		return new ChrSource() {
			private Iterator<Character> iter = Collections.emptyIterator();

			public char source() {
				Iterable<Character> iterable;
				while (!iter.hasNext())
					if ((iterable = source.source()) != null)
						iter = iterable.iterator();
					else
						return EMPTYVALUE;
				return iter.next();
			}
		};
	}

	public static <R> R fold(Fun<ChrObjPair<R>, R> fun0, R init, ChrSource source) {
		var fun1 = fun0.rethrow();
		char c;
		while ((c = source.source()) != EMPTYVALUE)
			init = fun1.apply(ChrObjPair.of(c, init));
		return init;
	}

	public static boolean isAll(ChrTest pred0, ChrSource source) {
		var pred1 = pred0.rethrow();
		char c;
		while ((c = source.source()) != EMPTYVALUE)
			if (!pred1.test(c))
				return false;
		return true;
	}

	public static boolean isAny(ChrTest pred0, ChrSource source) {
		var pred1 = pred0.rethrow();
		char c;
		while ((c = source.source()) != EMPTYVALUE)
			if (pred1.test(c))
				return true;
		return false;
	}

	public static Iterator<Character> iterator(ChrSource source) {
		return new Iterator<>() {
			private char next = EMPTYVALUE;

			public boolean hasNext() {
				if (next == EMPTYVALUE)
					next = source.source();
				return next != EMPTYVALUE;
			}

			public Character next() {
				var next0 = next;
				next = EMPTYVALUE;
				return next0;
			}

		};
	}

	public static Iterable<Character> iter(ChrSource source) {
		return () -> iterator(source);
	}

	public static <T1> Source<T1> map(Chr_Obj<T1> fun0, ChrSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c0 = source.source();
			return c0 != ChrFunUtil.EMPTYVALUE ? fun1.apply(c0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Chr_Obj<K> kf0, Chr_Obj<V> vf0, ChrSource source) {
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

	public static ChrSource mapChr(Chr_Chr fun0, ChrSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c = source.source();
			return c != ChrFunUtil.EMPTYVALUE ? fun1.apply(c) : ChrFunUtil.EMPTYVALUE;
		};
	}

	public static <V> ChrObjSource<V> mapChrObj(Chr_Obj<V> fun0, ChrSource source) {
		var fun1 = fun0.rethrow();
		return pair -> {
			var c = source.source();
			if (c != ChrFunUtil.EMPTYVALUE) {
				pair.update(c, fun1.apply(c));
				return true;
			} else
				return false;

		};
	}

	public static ChrSink nullSink() {
		return i -> {
		};
	}

	public static ChrSource nullSource() {
		return () -> EMPTYVALUE;
	}

	public static ChrSource snoc(char c, ChrSource source) {
		return new ChrSource() {
			private boolean isAppended = false;

			public char source() {
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
	public static Source<ChrSource> split(ChrTest fun0, ChrSource source) {
		var fun1 = fun0.rethrow();
		return new Source<>() {
			private char c = source.source();
			private boolean isAvail = c != EMPTYVALUE;
			private ChrSource source_ = () -> (isAvail = isAvail && (c = source.source()) != EMPTYVALUE) && !fun1.test(c) ? c
					: null;

			public ChrSource source() {
				return isAvail ? cons(c, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and make it into a source.
	 */
	public static ChrSource suck(Sink<ChrSink> fun) {
		var queue = new NullableSyncQueue<Character>();
		ChrSink enqueue = c -> enqueue(queue, c);

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

	private static void enqueue(NullableSyncQueue<Character> queue, char c) {
		try {
			queue.offer(c);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
		}
	}

}
