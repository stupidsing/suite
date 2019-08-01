package suite.primitive;

import static primal.statics.Fail.fail;

import java.util.Iterator;

import primal.NullableSyncQueue;
import primal.Verbs.Start;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Source2;
import primal.os.Log_;
import primal.primitive.ChrDblPredicate;
import primal.primitive.ChrDblSource;
import primal.primitive.ChrPrim.ChrTest;
import primal.primitive.DblPrim.DblTest;
import primal.primitive.adt.pair.ChrDblPair;
import primal.statics.Fail.InterruptedRuntimeException;

public class ChrDblFunUtil {

	public static ChrDblSource append(char key, double value, ChrDblSource source) {
		return new ChrDblSource() {
			private boolean isAppended = false;

			public boolean source2(ChrDblPair pair) {
				var b = !isAppended;
				if (b && !source.source2(pair)) {
					pair.update(key, value);
					isAppended = true;
				}
				return b;
			}
		};
	}

	public static <V> Source<ChrDblSource> chunk(int n, ChrDblSource source) {
		return new Source<>() {
			private ChrDblPair pair;
			private boolean isAvail;
			private int i;
			private ChrDblSource source_ = pair1 -> {
				var b = (isAvail = isAvail && source.source2(pair)) && ++i < n;
				if (b)
					pair1.update(pair.t0, pair.t1);
				else
					i = 0;
				return b;
			};

			{
				isAvail = source.source2(pair);
			}

			public ChrDblSource g() {
				return isAvail ? cons(pair.t0, pair.t1, source_) : null;
			}
		};
	}

	public static <V> ChrDblSource concat(Source<ChrDblSource> source) {
		return new ChrDblSource() {
			private ChrDblSource source2 = nullSource();

			public boolean source2(ChrDblPair pair) {
				var b = false;
				while (source2 != null && !(b = source2.source2(pair)))
					source2 = source.g();
				return b;
			}
		};
	}

	public static ChrDblSource cons(char key, double value, ChrDblSource source2) {
		return new ChrDblSource() {
			private boolean isFirst = true;

			public boolean source2(ChrDblPair pair) {
				if (!isFirst)
					return source2.source2(pair);
				else {
					isFirst = false;
					pair.update(key, value);
					return true;
				}
			}
		};
	}

	public static ChrDblSource filter(ChrDblPredicate fun0, ChrDblSource source2) {
		var fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0, pair.t1))
				;
			return b;
		};
	}

	public static <V> ChrDblSource filterKey(ChrTest fun0, ChrDblSource source2) {
		var fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t0))
				;
			return b;
		};
	}

	public static ChrDblSource filterValue(DblTest fun0, ChrDblSource source2) {
		var fun1 = fun0.rethrow();
		return pair -> {
			boolean b;
			while ((b = source2.source2(pair)) && !fun1.test(pair.t1))
				;
			return b;
		};
	}

	public static <K, V, R> R fold(Fun<Pair<R, ChrDblPair>, R> fun0, R init, ChrDblSource source2) {
		var fun1 = fun0.rethrow();
		var pair = ChrDblPair.of((char) 0, (double) 0);
		while (source2.source2(pair))
			init = fun1.apply(Pair.of(init, pair));
		return init;
	}

	public static boolean isAll(ChrDblPredicate pred0, ChrDblSource source2) {
		var pred1 = pred0.rethrow();
		var pair = ChrDblPair.of((char) 0, (double) 0);
		while (source2.source2(pair))
			if (!pred1.test(pair.t0, pair.t1))
				return false;
		return true;
	}

	public static <V> boolean isAny(ChrDblPredicate pred0, ChrDblSource source2) {
		var pred1 = pred0.rethrow();
		var pair = ChrDblPair.of((char) 0, (double) 0);
		while (source2.source2(pair))
			if (pred1.test(pair.t0, pair.t1))
				return true;
		return false;
	}

	public static <V> Iterator<ChrDblPair> iterator(ChrDblSource source2) {
		return new Iterator<>() {
			private ChrDblPair next = null;

			public boolean hasNext() {
				if (next == null) {
					var next1 = ChrDblPair.of((char) 0, (double) 0);
					if (source2.source2(next1))
						next = next1;
				}
				return next != null;
			}

			public ChrDblPair next() {
				var next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <V> Iterable<ChrDblPair> iter(ChrDblSource source2) {
		return () -> iterator(source2);
	}

	public static <T> Source<T> map(ChrDbl_Obj<T> fun0, ChrDblSource source2) {
		var fun1 = fun0.rethrow();
		var pair = ChrDblPair.of((char) 0, (double) 0);
		return () -> source2.source2(pair) ? fun1.apply(pair.t0, pair.t1) : null;
	}

	public static <K1, V1, T> Source2<K1, V1> map2(ChrDbl_Obj<K1> kf0, ChrDbl_Obj<V1> vf0, ChrDblSource source2) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		var pair1 = ChrDblPair.of((char) 0, (double) 0);
		return pair -> {
			var b = source2.source2(pair1);
			if (b)
				pair.update(kf1.apply(pair1.t0, pair1.t1), vf1.apply(pair1.t0, pair1.t1));
			return b;
		};
	}

	public static ChrDblSource mapChrDbl(ChrDbl_Chr kf0, ChrDbl_Dbl vf0, ChrDblSource source2) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		var pair1 = ChrDblPair.of((char) 0, (double) 0);
		return pair -> {
			var b = source2.source2(pair1);
			if (b)
				pair.update(kf1.apply(pair1.t0, pair1.t1), vf1.apply(pair1.t0, pair1.t1));
			return b;
		};
	}

	public static <I> Sink<I> nullSink() {
		return i -> {
		};
	}

	public static <V> ChrDblSource nullSource() {
		return pair -> false;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must not be
	 * skipped.
	 */
	public static Source<ChrDblSource> split(ChrDblPredicate fun0, ChrDblSource source2) {
		var fun1 = fun0.rethrow();
		return new Source<>() {
			private ChrDblPair pair = ChrDblPair.of((char) 0, (double) 0);
			private boolean isAvailable;
			private ChrDblSource source2_ = pair_ -> (isAvailable &= source2.source2(pair_)) && !fun1.test(pair.t0, pair.t1);

			{
				isAvailable = source2.source2(pair);
			}

			public ChrDblSource g() {
				return isAvailable ? cons(pair.t0, pair.t1, source2_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and make it into a source.
	 */
	public static <V> ChrDblSource suck(Sink<Sink<ChrDblPair>> fun) {
		var queue = new NullableSyncQueue<ChrDblPair>();
		Sink<ChrDblPair> enqueue = pair -> enqueue(queue, pair);

		var thread = Start.thread(() -> {
			try {
				fun.f(enqueue);
			} finally {
				enqueue(queue, null);
			}
		});

		return pair -> {
			try {
				var p = queue.take();
				var b = p != null;
				if (b)
					pair.update(p.t0, p.t1);
				return b;
			} catch (InterruptedException | InterruptedRuntimeException ex) {
				thread.interrupt();
				return fail(ex);
			}
		};
	}

	private static <T> void enqueue(NullableSyncQueue<T> queue, T t) {
		try {
			queue.offer(t);
		} catch (InterruptedException ex) {
			Log_.error(ex);
		}
	}

}
