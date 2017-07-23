package suite.primitive;

import java.util.Collections;
import java.util.Iterator;

import suite.os.LogUtil;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.ChrPredicate;
import suite.primitive.ChrPrimitives.ChrSink;
import suite.primitive.ChrPrimitives.ChrSource;
import suite.primitive.ChrPrimitives.Chr_Obj;
import suite.primitive.adt.pair.ChrObjPair;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Thread_;

public class ChrFunUtil {

	public static char EMPTYVALUE = Character.MIN_VALUE;

	public static ChrSource append(char c, ChrSource source) {
		return new ChrSource() {
			private boolean isAppended = false;

			public char source() {
				if (!isAppended) {
					char c_ = source.source();
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

	public static Source<ChrSource> chunk(int n, ChrSource source) {
		return new Source<ChrSource>() {
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
				char c = EMPTYVALUE;
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

	public static ChrSource filter(ChrPredicate fun0, ChrSource source) {
		ChrPredicate fun1 = fun0.rethrow();
		return () -> {
			char c = EMPTYVALUE;
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
		Fun<ChrObjPair<R>, R> fun1 = fun0.rethrow();
		char c;
		while ((c = source.source()) != EMPTYVALUE)
			init = fun1.apply(ChrObjPair.of(c, init));
		return init;
	}

	public static boolean isAll(ChrPredicate pred0, ChrSource source) {
		ChrPredicate pred1 = pred0.rethrow();
		char c;
		while ((c = source.source()) != EMPTYVALUE)
			if (!pred1.test(c))
				return false;
		return true;
	}

	public static boolean isAny(ChrPredicate pred0, ChrSource source) {
		ChrPredicate pred1 = pred0.rethrow();
		char c;
		while ((c = source.source()) != EMPTYVALUE)
			if (pred1.test(c))
				return true;
		return false;
	}

	public static Iterator<Character> iterator(ChrSource source) {
		return new Iterator<Character>() {
			private char next = EMPTYVALUE;

			public boolean hasNext() {
				if (next == EMPTYVALUE)
					next = source.source();
				return next != EMPTYVALUE;
			}

			public Character next() {
				char next0 = next;
				next = EMPTYVALUE;
				return next0;
			}

		};
	}

	public static Iterable<Character> iter(ChrSource source) {
		return () -> iterator(source);
	}

	public static <T1> Source<T1> map(Chr_Obj<T1> fun0, ChrSource source) {
		Chr_Obj<T1> fun1 = fun0.rethrow();
		return () -> {
			char c0 = source.source();
			return c0 != ChrFunUtil.EMPTYVALUE ? fun1.apply(c0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Chr_Obj<K> kf0, Chr_Obj<V> vf0, ChrSource source) {
		Chr_Obj<K> kf1 = kf0.rethrow();
		Chr_Obj<V> vf1 = vf0.rethrow();
		return pair -> {
			char c = source.source();
			boolean b = c != EMPTYVALUE;
			if (b) {
				pair.t0 = kf1.apply(c);
				pair.t1 = vf1.apply(c);
			}
			return b;
		};
	}

	public static ChrSource mapChr(Chr_Chr fun0, ChrSource source) {
		Chr_Chr fun1 = fun0.rethrow();
		return () -> {
			char c = source.source();
			return c != ChrFunUtil.EMPTYVALUE ? fun1.apply(c) : ChrFunUtil.EMPTYVALUE;
		};
	}

	public static <V> ChrObjSource<V> mapChrObj(Chr_Obj<V> fun0, ChrSource source) {
		Chr_Obj<V> fun1 = fun0.rethrow();
		return pair -> {
			char c = source.source();
			if (c != ChrFunUtil.EMPTYVALUE) {
				pair.t0 = c;
				pair.t1 = fun1.apply(c);
				return true;
			} else
				return false;

		};
	}

	public static <T> Source<T> mapNonNull(Chr_Obj<T> fun0, ChrSource source) {
		Chr_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				char c;
				T t;
				while ((c = source.source()) != ChrFunUtil.EMPTYVALUE)
					if ((t = fun1.apply(c)) != null)
						return t;
				return null;
			}
		};
	}

	public static ChrSink nullSink() {
		return i -> {
		};
	}

	public static ChrSource nullSource() {
		return () -> EMPTYVALUE;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must not be
	 * skipped.
	 */
	public static Source<ChrSource> split(ChrPredicate fun0, ChrSource source) {
		ChrPredicate fun1 = fun0.rethrow();
		return new Source<ChrSource>() {
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
	 * Sucks data from a sink and produce into a source.
	 */
	public static ChrSource suck(Sink<ChrSink> fun) {
		NullableSyncQueue<Character> queue = new NullableSyncQueue<>();
		ChrSink enqueue = c -> enqueue(queue, c);

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

	private static void enqueue(NullableSyncQueue<Character> queue, char c) {
		try {
			queue.offer(c);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
		}
	}

}
