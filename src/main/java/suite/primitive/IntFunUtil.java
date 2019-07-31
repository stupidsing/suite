package suite.primitive;

import static primal.statics.Fail.fail;

import java.util.Collections;
import java.util.Iterator;

import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Source2;
import primal.os.Log_;
import primal.statics.Fail.InterruptedRuntimeException;
import suite.primitive.IntPrimitives.IntObjSource;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.IntPrimitives.IntSource;
import suite.primitive.IntPrimitives.IntTest;
import suite.primitive.IntPrimitives.Int_Obj;
import suite.primitive.adt.pair.IntObjPair;
import suite.util.NullableSyncQueue;
import suite.util.Thread_;

public class IntFunUtil {

	public static int EMPTYVALUE = Integer.MIN_VALUE;

	public static Source<IntSource> chunk(int n, IntSource source) {
		return new Source<>() {
			private int c = source.g();
			private boolean isAvail = c != EMPTYVALUE;
			private int i;
			private IntSource source_ = () -> {
				if ((isAvail = isAvail && (c = source.g()) != EMPTYVALUE) && ++i < n)
					return c;
				else {
					i = 0;
					return EMPTYVALUE;
				}
			};

			public IntSource g() {
				return isAvail ? cons(c, source_) : null;
			}
		};
	}

	public static IntSource concat(Source<IntSource> source) {
		return new IntSource() {
			private IntSource source0 = nullSource();

			public int g() {
				var c = EMPTYVALUE;
				while (source0 != null && (c = source0.g()) == EMPTYVALUE)
					source0 = source.g();
				return c;
			}
		};
	}

	public static IntSource cons(int c, IntSource source) {
		return new IntSource() {
			private boolean isFirst = true;

			public int g() {
				if (!isFirst)
					return source.g();
				else {
					isFirst = false;
					return c;
				}
			}
		};
	}

	public static IntSource filter(IntTest fun0, IntSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c = EMPTYVALUE;
			while ((c = source.g()) != EMPTYVALUE && !fun1.test(c))
				;
			return c;
		};
	}

	public static IntSource flatten(Source<Iterable<Integer>> source) {
		return new IntSource() {
			private Iterator<Integer> iter = Collections.emptyIterator();

			public int g() {
				Iterable<Integer> iterable;
				while (!iter.hasNext())
					if ((iterable = source.g()) != null)
						iter = iterable.iterator();
					else
						return EMPTYVALUE;
				return iter.next();
			}
		};
	}

	public static <R> R fold(Fun<IntObjPair<R>, R> fun0, R init, IntSource source) {
		var fun1 = fun0.rethrow();
		int c;
		while ((c = source.g()) != EMPTYVALUE)
			init = fun1.apply(IntObjPair.of(c, init));
		return init;
	}

	public static boolean isAll(IntTest pred0, IntSource source) {
		var pred1 = pred0.rethrow();
		int c;
		while ((c = source.g()) != EMPTYVALUE)
			if (!pred1.test(c))
				return false;
		return true;
	}

	public static boolean isAny(IntTest pred0, IntSource source) {
		var pred1 = pred0.rethrow();
		int c;
		while ((c = source.g()) != EMPTYVALUE)
			if (pred1.test(c))
				return true;
		return false;
	}

	public static Iterator<Integer> iterator(IntSource source) {
		return new Iterator<>() {
			private int next = EMPTYVALUE;

			public boolean hasNext() {
				if (next == EMPTYVALUE)
					next = source.g();
				return next != EMPTYVALUE;
			}

			public Integer next() {
				var next0 = next;
				next = EMPTYVALUE;
				return next0;
			}

		};
	}

	public static Iterable<Integer> iter(IntSource source) {
		return () -> iterator(source);
	}

	public static <T1> Source<T1> map(Int_Obj<T1> fun0, IntSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c0 = source.g();
			return c0 != IntFunUtil.EMPTYVALUE ? fun1.apply(c0) : null;
		};
	}

	public static <K, V> Source2<K, V> map2(Int_Obj<K> kf0, Int_Obj<V> vf0, IntSource source) {
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

	public static IntSource mapInt(Int_Int fun0, IntSource source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var c = source.g();
			return c != IntFunUtil.EMPTYVALUE ? fun1.apply(c) : IntFunUtil.EMPTYVALUE;
		};
	}

	public static <V> IntObjSource<V> mapIntObj(Int_Obj<V> fun0, IntSource source) {
		var fun1 = fun0.rethrow();
		return pair -> {
			var c = source.g();
			if (c != IntFunUtil.EMPTYVALUE) {
				pair.update(c, fun1.apply(c));
				return true;
			} else
				return false;

		};
	}

	public static IntSink nullSink() {
		return i -> {
		};
	}

	public static IntSource nullSource() {
		return () -> EMPTYVALUE;
	}

	public static IntSource snoc(int c, IntSource source) {
		return new IntSource() {
			private boolean isAppended = false;

			public int g() {
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
	public static Source<IntSource> split(IntTest fun0, IntSource source) {
		var fun1 = fun0.rethrow();
		return new Source<>() {
			private int c = source.g();
			private boolean isAvail = c != EMPTYVALUE;
			private IntSource source_ = () -> (isAvail = isAvail && (c = source.g()) != EMPTYVALUE) && !fun1.test(c) ? c : null;

			public IntSource g() {
				return isAvail ? cons(c, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and make it into a source.
	 */
	public static IntSource suck(Sink<IntSink> fun) {
		var queue = new NullableSyncQueue<Integer>();
		IntSink enqueue = c -> enqueue(queue, c);

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

	private static void enqueue(NullableSyncQueue<Integer> queue, int c) {
		try {
			queue.offer(c);
		} catch (InterruptedException ex) {
			Log_.error(ex);
		}
	}

}
