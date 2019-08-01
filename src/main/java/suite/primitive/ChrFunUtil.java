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
import primal.primitive.ChrPrim;
import primal.primitive.ChrPrim.ChrSink;
import primal.primitive.ChrPrim.ChrSource;
import primal.primitive.ChrPrim.ChrTest;
import primal.primitive.adt.pair.ChrObjPair;
import primal.statics.Fail.InterruptedRuntimeException;
import suite.primitive.ChrPrimitives.ChrObjSource;
import suite.primitive.ChrPrimitives.Chr_Obj;

public class ChrFunUtil {

	public static Source<ChrSource> chunk(int n, ChrSource source) {
		return new Source<>() {
			private char c = source.g();
			private boolean isAvail = c != ChrPrim.EMPTYVALUE;
			private int i;
			private ChrSource source_ = () -> {
				if ((isAvail = isAvail && (c = source.g()) != ChrPrim.EMPTYVALUE) && ++i < n)
					return c;
				else {
					i = 0;
					return ChrPrim.EMPTYVALUE;
				}
			};

			public ChrSource g() {
				return isAvail ? cons(c, source_) : null;
			}
		};
	}

	public static ChrSource concat(Source<ChrSource> source) {
		return new ChrSource() {
			private ChrSource source0 = nullSource();

			public char g() {
				var c = ChrPrim.EMPTYVALUE;
				while (source0 != null && (c = source0.g()) == ChrPrim.EMPTYVALUE)
					source0 = source.g();
				return c;
			}
		};
	}

	public static ChrSource cons(char c, ChrSource source) {
		return new ChrSource() {
			private boolean isFirst = true;

			public char g() {
				if (!isFirst)
					return source.g();
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
			var c = ChrPrim.EMPTYVALUE;
			while ((c = source.g()) != ChrPrim.EMPTYVALUE && !fun1.test(c))
				;
			return c;
		};
	}

	public static ChrSource flatten(Source<Iterable<Character>> source) {
		return new ChrSource() {
			private Iterator<Character> iter = Collections.emptyIterator();

			public char g() {
				Iterable<Character> iterable;
				while (!iter.hasNext())
					if ((iterable = source.g()) != null)
						iter = iterable.iterator();
					else
						return ChrPrim.EMPTYVALUE;
				return iter.next();
			}
		};
	}

	public static <R> R fold(Fun<ChrObjPair<R>, R> fun0, R init, ChrSource source) {
		var fun1 = fun0.rethrow();
		char c;
		while ((c = source.g()) != ChrPrim.EMPTYVALUE)
			init = fun1.apply(ChrObjPair.of(c, init));
		return init;
	}

	public static boolean isAll(ChrTest pred0, ChrSource source) {
		var pred1 = pred0.rethrow();
		char c;
		while ((c = source.g()) != ChrPrim.EMPTYVALUE)
			if (!pred1.test(c))
				return false;
		return true;
	}

	public static boolean isAny(ChrTest pred0, ChrSource source) {
		var pred1 = pred0.rethrow();
		char c;
		while ((c = source.g()) != ChrPrim.EMPTYVALUE)
			if (pred1.test(c))
				return true;
		return false;
	}

	public static Iterator<Character> iterator(ChrSource source) {
		return new Iterator<>() {
			private char next = ChrPrim.EMPTYVALUE;

			public boolean hasNext() {
				if (next == ChrPrim.EMPTYVALUE)
					next = source.g();
				return next != ChrPrim.EMPTYVALUE;
			}

			public Character next() {
				var next0 = next;
				next = ChrPrim.EMPTYVALUE;
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
			var c0 = source.g();
			return c0 != ChrPrim.EMPTYVALUE ? fun1.apply(c0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Chr_Obj<K> kf0, Chr_Obj<V> vf0, ChrSource source) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		return pair -> {
			var c = source.g();
			var b = c != ChrPrim.EMPTYVALUE;
			if (b)
				pair.update(kf1.apply(c), vf1.apply(c));
			return b;
		};
	}

	public static ChrSource mapChr(Chr_Chr fun0, ChrSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c = source.g();
			return c != ChrPrim.EMPTYVALUE ? fun1.apply(c) : ChrPrim.EMPTYVALUE;
		};
	}

	public static <V> ChrObjSource<V> mapChrObj(Chr_Obj<V> fun0, ChrSource source) {
		var fun1 = fun0.rethrow();
		return pair -> {
			var c = source.g();
			if (c != ChrPrim.EMPTYVALUE) {
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
		return () -> ChrPrim.EMPTYVALUE;
	}

	public static ChrSource snoc(char c, ChrSource source) {
		return new ChrSource() {
			private boolean isAppended = false;

			public char g() {
				if (!isAppended) {
					var c_ = source.g();
					if (c_ != ChrPrim.EMPTYVALUE)
						return c_;
					else {
						isAppended = true;
						return c;
					}
				} else
					return ChrPrim.EMPTYVALUE;
			}
		};
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must not be
	 * skipped.
	 */
	public static Source<ChrSource> split(ChrTest fun0, ChrSource source) {
		var fun1 = fun0.rethrow();
		return new Source<>() {
			private char c = source.g();
			private boolean isAvail = c != ChrPrim.EMPTYVALUE;
			private ChrSource source_ = () -> (isAvail = isAvail && (c = source.g()) != ChrPrim.EMPTYVALUE) && !fun1.test(c) ? c : null;

			public ChrSource g() {
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

		var thread = Start.thread(() -> {
			try {
				fun.f(enqueue);
			} finally {
				enqueue(queue, ChrPrim.EMPTYVALUE);
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

	private static void enqueue(NullableSyncQueue<Character> queue, char c) {
		try {
			queue.offer(c);
		} catch (InterruptedException ex) {
			Log_.error(ex);
		}
	}

}
