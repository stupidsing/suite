package suite.util;

import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;

import suite.adt.Pair;
import suite.os.LogUtil;
import suite.util.FunUtil2.Source2;

public class FunUtil {

	@FunctionalInterface
	public interface Source<O> {
		public O source();
	}

	@FunctionalInterface
	public interface Sink<I> {
		public void sink(I i);
	}

	@FunctionalInterface
	public interface Fun<I, O> extends Function<I, O> {
	}

	public static <T> Source<Source<T>> chunk(int n, Source<T> source) {
		return new Source<Source<T>>() {
			private T t = source.source();
			private boolean isAvail = t != null;
			private int i;
			private Source<T> source_ = () -> {
				if ((isAvail = isAvail && (t = source.source()) != null) && ++i < n)
					return t;
				else {
					i = 0;
					return null;
				}
			};

			public Source<T> source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	public static <T> Source<T> concat(Source<Source<T>> source) {
		return new Source<T>() {
			private Source<T> source0 = nullSource();

			public T source() {
				T t = null;
				while (source0 != null && (t = source0.source()) == null)
					source0 = source.source();
				return t;
			}
		};
	}

	public static <T> Source<T> cons(T t, Source<T> source) {
		return new Source<T>() {
			private boolean isFirst = true;

			public T source() {
				if (!isFirst)
					return source.source();
				else {
					isFirst = false;
					return t;
				}
			}
		};
	}

	public static <T> Source<T> filter(Predicate<T> fun0, Source<T> source) {
		Predicate<T> fun1 = Rethrow.predicate(fun0);
		return () -> {
			T t = null;
			while ((t = source.source()) != null && !fun1.test(t))
				;
			return t;
		};
	}

	public static <T, R> R fold(Fun<Pair<R, T>, R> fun0, R init, Source<T> source) {
		Fun<Pair<R, T>, R> fun1 = Rethrow.fun(fun0);
		T t;
		while ((t = source.source()) != null)
			init = fun1.apply(Pair.of(init, t));
		return init;
	}

	public static <T> boolean isAll(Predicate<T> pred0, Source<T> source) {
		Predicate<T> pred1 = Rethrow.predicate(pred0);
		T t;
		while ((t = source.source()) != null)
			if (!pred1.test(t))
				return false;
		return true;
	}

	public static <T> boolean isAny(Predicate<T> pred0, Source<T> source) {
		Predicate<T> pred1 = Rethrow.predicate(pred0);
		T t;
		while ((t = source.source()) != null)
			if (pred1.test(t))
				return true;
		return false;
	}

	public static <T> Iterator<T> iterator(Source<T> source) {
		return new Iterator<T>() {
			private T next = null;

			public boolean hasNext() {
				if (next == null)
					next = source.source();
				return next != null;
			}

			public T next() {
				T next0 = next;
				next = null;
				return next0;
			}
		};
	}

	public static <T> Iterable<T> iter(Source<T> source) {
		return () -> iterator(source);
	}

	public static <T0, T1> Source<T1> map(Fun<T0, T1> fun0, Source<T0> source) {
		Fun<T0, T1> fun1 = Rethrow.fun(fun0);
		return () -> {
			T0 t0 = source.source();
			return t0 != null ? fun1.apply(t0) : null;
		};
	}

	public static <T, K, V> Source2<K, V> map2(Fun<T, K> kf0, Fun<T, V> vf0, Source<T> source) {
		Fun<T, K> kf1 = Rethrow.fun(kf0);
		Fun<T, V> vf1 = Rethrow.fun(vf0);
		return pair -> {
			T t = source.source();
			if (t != null) {
				pair.t0 = kf1.apply(t);
				pair.t1 = vf1.apply(t);
				return true;
			} else
				return false;
		};
	}

	public static <I> Sink<I> nullSink() {
		return i -> {
		};
	}

	public static <O> Source<O> nullSource() {
		return () -> null;
	}

	/**
	 * Problematic split: all data must be read, i.e. the children lists must
	 * not be skipped.
	 */
	public static <T> Source<Source<T>> split(Predicate<T> fun0, Source<T> source) {
		Predicate<T> fun1 = Rethrow.predicate(fun0);
		return new Source<Source<T>>() {
			private T t = source.source();
			private boolean isAvail = t != null;
			private Source<T> source_ = () -> (isAvail = isAvail && (t = source.source()) != null) && !fun1.test(t) ? t : null;

			public Source<T> source() {
				return isAvail ? cons(t, source_) : null;
			}
		};
	}

	/**
	 * Sucks data from a sink and produce into a source.
	 */
	public static <T> Source<T> suck(Sink<Sink<T>> fun) {
		NullableSynchronousQueue<T> queue = new NullableSynchronousQueue<>();
		Sink<T> enqueue = t -> enqueue(queue, t);

		Thread thread = Util.startThread(() -> {
			try {
				fun.sink(enqueue);
			} finally {
				enqueue(queue, null);
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

	private static <T> void enqueue(NullableSynchronousQueue<T> queue, T t) {
		try {
			queue.offer(t);
		} catch (InterruptedException ex) {
			LogUtil.error(ex);
		}
	}

}
