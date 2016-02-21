package suite.util;

import java.util.Iterator;
import java.util.function.Function;

import suite.adt.Pair;
import suite.os.LogUtil;

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

	public static <T> Source<Source<T>> chunk(Source<T> source, int n) {
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

	public static <T> Source<T> filter(Fun<T, Boolean> fun, Source<T> source) {
		return () -> {
			T t = null;
			while ((t = source.source()) != null && !fun.apply(t))
				;
			return t;
		};
	}

	public static <T, R> R fold(Fun<Pair<R, T>, R> fun, R init, Source<T> source) {
		T t;
		while ((t = source.source()) != null)
			init = fun.apply(Pair.of(init, t));
		return init;
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

	public static <T0, T1> Source<T1> map(Fun<T0, T1> fun, Source<T0> source) {
		return () -> {
			T0 e = source.source();
			return e != null ? fun.apply(e) : null;
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
	public static <T> Source<Source<T>> split(Source<T> source, Fun<T, Boolean> fun) {
		return new Source<Source<T>>() {
			private T t = source.source();
			private boolean isAvail = t != null;
			private Source<T> source_ = () -> (isAvail = isAvail && (t = source.source()) != null) && !fun.apply(t) ? t : null;

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
