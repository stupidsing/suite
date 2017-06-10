package suite.primitive;

import java.util.Collections;
import java.util.Iterator;

import suite.adt.pair.ChrObjPair;
import suite.os.LogUtil;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.ChrPredicate;
import suite.primitive.ChrPrimitives.ChrSink;
import suite.primitive.ChrPrimitives.ChrSource;
import suite.primitive.ChrPrimitives.Chr_Obj;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Source2;
import suite.util.NullableSyncQueue;
import suite.util.Rethrow;
import suite.util.Thread_;

public class ChrFunUtil {

	public static char EMPTYVALUE = Character.MIN_VALUE;

	public static ChrSource append(char t, ChrSource source) {
		return new ChrSource() {
			private boolean isAppended = false;

			public char source() {
				if (!isAppended) {
					char t_ = source.source();
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

	public static Source<ChrSource> chunk(int n, ChrSource source) {
		return new Source<ChrSource>() {
			private char t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private int i;
			private ChrSource source_ = () -> {
				if ((isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && ++i < n)
					return t;
				else {
					i = 0;
					return EMPTYVALUE;
				}
			};

			public ChrSource source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	public static ChrSource concat(Source<ChrSource> source) {
		return new ChrSource() {
			private ChrSource source0 = nullSource();

			public char source() {
				char t = EMPTYVALUE;
				while (source0 != null && (t = source0.source()) == EMPTYVALUE)
					source0 = source.source();
				return t;
			}
		};
	}

	public static ChrSource cons(char t, ChrSource source) {
		return new ChrSource() {
			private boolean isFirst = true;

			public char source() {
				if (!isFirst)
					return source.source();
				else {
					isFirst = false;
					return t;
				}
			}
		};
	}

	public static ChrSource filter(ChrPredicate fun0, ChrSource source) {
		ChrPredicate fun1 = fun0.rethrow();
		return () -> {
			char t = EMPTYVALUE;
			while ((t = source.source()) != EMPTYVALUE && !fun1.test(t))
				;
			return t;
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
		Fun<ChrObjPair<R>, R> fun1 = Rethrow.fun(fun0);
		char t;
		while ((t = source.source()) != EMPTYVALUE)
			init = fun1.apply(ChrObjPair.of(t, init));
		return init;
	}

	public static boolean isAll(ChrPredicate pred0, ChrSource source) {
		ChrPredicate pred1 = pred0.rethrow();
		char t;
		while ((t = source.source()) != EMPTYVALUE)
			if (!pred1.test(t))
				return false;
		return true;
	}

	public static boolean isAny(ChrPredicate pred0, ChrSource source) {
		ChrPredicate pred1 = pred0.rethrow();
		char t;
		while ((t = source.source()) != EMPTYVALUE)
			if (pred1.test(t))
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
			char t0 = source.source();
			return t0 != ChrFunUtil.EMPTYVALUE ? fun1.apply(t0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Chr_Obj<K> kf0, Chr_Obj<V> vf0, ChrSource source) {
		Chr_Obj<K> kf1 = kf0.rethrow();
		Chr_Obj<V> vf1 = vf0.rethrow();
		return pair -> {
			char t = source.source();
			boolean b = t != EMPTYVALUE;
			if (b) {
				pair.t0 = kf1.apply(t);
				pair.t1 = vf1.apply(t);
			}
			return b;
		};
	}

	public static ChrSource mapChr(Chr_Chr fun0, ChrSource source) {
		Chr_Chr fun1 = fun0.rethrow();
		return () -> {
			char t = source.source();
			return t != ChrFunUtil.EMPTYVALUE ? fun1.apply(t) : ChrFunUtil.EMPTYVALUE;
		};
	}

	public static <V> ChrObjSource<V> mapChrObj(Chr_Obj<V> fun0, ChrSource source) {
		Chr_Obj<V> fun1 = fun0.rethrow();
		return pair -> {
			char t = source.source();
			if (t != ChrFunUtil.EMPTYVALUE) {
				pair.t0 = t;
				pair.t1 = fun1.apply(t);
				return true;
			} else
				return false;

		};
	}

	public static <T> Source<T> mapNonNull(Chr_Obj<T> fun0, ChrSource source) {
		Chr_Obj<T> fun1 = fun0.rethrow();
		return new Source<T>() {
			public T source() {
				char t0;
				T t1;
				while ((t0 = source.source()) != ChrFunUtil.EMPTYVALUE)
					if ((t1 = fun1.apply(t0)) != null)
						return t1;
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
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static Source<ChrSource> split(ChrPredicate fun0, ChrSource source) {
		ChrPredicate fun1 = fun0.rethrow();
		return new Source<ChrSource>() {
			private char t = source.source();
			private boolean isAvail = t != EMPTYVALUE;
			private ChrSource source_ = () -> (isAvail = isAvail && (t = source.source()) != EMPTYVALUE) && !fun1.test(t) ? t
					: null;

			public ChrSource source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static ChrSource suck(Sink<ChrSink> fun) {
		NullableSyncQueue<Character> queue = new NullableSyncQueue<>();
		ChrSink enqueue = t -> enqueue(queue, t);

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

	private static void enqueue(NullableSyncQueue<Character> queue, char t) {
		try {
			queue.offer(t);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
		}
	}

}
