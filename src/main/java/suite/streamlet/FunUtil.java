package suite.streamlet;

import static primal.statics.Fail.fail;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Predicate;

import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Source2;
import primal.os.Log_;
import primal.statics.Fail.InterruptedRuntimeException;
import primal.statics.Rethrow;
import suite.util.NullableSyncQueue;
import suite.util.Thread_;

public class FunUtil {

	public static <T> Source<Source<T>> chunk(int n, Source<T> source) {
		return new Source<>() {
			private T t = source.g();
			private boolean isAvail = t != null;
			private int i;
			private Source<T> source_ = () -> {
				if ((isAvail = isAvail && (t = source.g()) != null) && ++i < n)
					return t;
				else {
					i = 0;
					return null;
				}
			};

			public Source<T> g() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	public static <T> Source<T> concat(Source<Source<T>> source) {
		return new Source<>() {
			private Source<T> source0 = nullSource();

			public T g() {
				T t = null;
				while (source0 != null && (t = source0.g()) == null)
					source0 = source.g();
				return t;
			}
		};
	}

	public static <T> Source<T> cons(T t, Source<T> source) {
		return new Source<>() {
			private boolean isFirst = true;

			public T g() {
				if (!isFirst)
					return source.g();
				else {
					isFirst = false;
					return t;
				}
			}
		};
	}

	public static <T> Source<T> filter(Predicate<T> fun0, Source<T> source) {
		var fun1 = Rethrow.predicate(fun0);
		return () -> {
			T t = null;
			while ((t = source.g()) != null && !fun1.test(t))
				;
			return t;
		};
	}

	public static <T> Source<T> flatten(Source<Iterable<T>> source) {
		return new Source<>() {
			private Iterator<T> iter = Collections.emptyIterator();

			public T g() {
				Iterable<T> iterable;
				while (!iter.hasNext())
					if ((iterable = source.g()) != null)
						iter = iterable.iterator();
					else
						return null;
				return iter.next();
			}
		};
	}

	public static <T, R> R fold(Fun<Pair<R, T>, R> fun0, R init, Source<T> source) {
		var fun1 = fun0.rethrow();
		T t;
		while ((t = source.g()) != null)
			init = fun1.apply(Pair.of(init, t));
		return init;
	}

	public static <T> boolean isAll(Predicate<T> pred0, Source<T> source) {
		var pred1 = Rethrow.predicate(pred0);
		T t;
		while ((t = source.g()) != null)
			if (!pred1.test(t))
				return false;
		return true;
	}

	public static <T> boolean isAny(Predicate<T> pred0, Source<T> source) {
		var pred1 = Rethrow.predicate(pred0);
		T t;
		while ((t = source.g()) != null)
			if (pred1.test(t))
				return true;
		return false;
	}

	public static <T> Iterator<T> iterator(Source<T> source) {
		return new Iterator<>() {
			private T next = null;

			public boolean hasNext() {
				if (next == null)
					next = source.g();
				return next != null;
			}

			public T next() {
				var next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <T> Iterable<T> iter(Source<T> source) {
		return () -> iterator(source);
	}

	public static <T0, T1> Source<T1> map(Fun<T0, T1> fun0, Source<T0> source) {
		var fun1 = fun0.rethrow();
		return () -> {
			var t0 = source.g();
			return t0 != null ? fun1.apply(t0) : null;
		};
	}

	public static <T, K, V> Source2<K, V> map2(Fun<T, K> kf0, Fun<T, V> vf0, Source<T> source) {
		var kf1 = kf0.rethrow();
		var vf1 = vf0.rethrow();
		return pair -> {
			var t = source.g();
			boolean b = t != null;
			if (b)
				pair.update(kf1.apply(t), vf1.apply(t));
			return b;
		};
	}

	public static <I> Sink<I> nullSink() {
		return i -> {
		};
	}

	public static <O> Source<O> nullSource() {
		return () -> null;
	}

	public static <T> Source<T> snoc(T t, Source<T> source) {
		return new Source<>() {
			private boolean isAppended = false;

			public T g() {
				if (!isAppended) {
					var t_ = source.g();
					if (t_ != null)
						return t_;
					else {
						isAppended = true;
						return t;
					}
				} else
					return null;
			}
		};
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must not be
	 * skipped.
	 */
	public static <T> Source<Source<T>> split(Predicate<T> fun0, Source<T> source) {
		var fun1 = Rethrow.predicate(fun0);
		return new Source<>() {
			private T t = source.g();
			private boolean isAvail = t != null;
			private Source<T> source_ = () -> (isAvail = isAvail && (t = source.g()) != null) && !fun1.test(t) ? t : null;

			public Source<T> g() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and make it into a source.
	 */
	public static <T> Source<T> suck(Sink<Sink<T>> fun) {
		var queue = new NullableSyncQueue<T>();
		Sink<T> enqueue = t -> enqueue(queue, t);

		var thread = Thread_.startThread(() -> {
			try {
				fun.f(enqueue);
			} finally {
				enqueue(queue, null);
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

	private static <T> void enqueue(NullableSyncQueue<T> queue, T t) {
		try {
			queue.offer(t);
		} catch (InterruptedException ex) {
			Log_.error(ex);
		}
	}

}
