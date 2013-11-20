package suite.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class FunUtil {

	public interface Source<O> {
		public O source();
	}

	public interface Sink<I> {
		public void sink(I i);
	}

	public interface Fun<I, O> {
		public O apply(I i);
	}

	public interface FunEx<I, O, Ex extends Exception> {
		public O apply(I i) throws Ex;
	}

	public static class Sinks<I> implements Sink<I> {
		private Collection<Sink<I>> sinks = new ArrayList<>();

		public void sink(I i) {
			for (Sink<I> sink : sinks)
				sink.sink(i);
		}

		public void add(Sink<I> sink) {
			sinks.add(sink);
		}
	}

	@SafeVarargs
	public static <O> Source<O> asSource(final O... array) {
		return asSource(Arrays.asList(array));
	}

	public static <O> Source<O> asSource(final List<O> list) {
		return new Source<O>() {
			private int i = 0;

			public O source() {
				return i < list.size() ? list.get(i++) : null;
			}
		};
	}

	public static <T> Source<T> concat(final Source<Source<T>> source) {
		return new Source<T>() {
			private Source<T> source0 = nullSource();

			public T source() {
				T e = null;
				while (source0 != null && (e = source0.source()) == null)
					source0 = source.source();
				return e;
			}
		};
	}

	public static <T> Source<T> cons(final T t, final Source<T> source) {
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

	public static <T> Source<T> filter(final Fun<T, Boolean> fun, final Source<T> source) {
		return new Source<T>() {
			public T source() {
				T e = null;
				while ((e = source.source()) != null && !fun.apply(e))
					;
				return e;
			}
		};
	}

	public static <T> Iterator<T> iterator(final Source<T> source) {
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

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public static <T> Iterable<T> iter(Source<T> source) {
		return Util.iter(iterator(source));
	}

	public static <T0, T1> Source<T1> map(final Fun<T0, T1> fun, final Source<T0> source) {
		return new Source<T1>() {
			public T1 source() {
				T0 e = source.source();
				return e != null ? fun.apply(e) : null;
			}
		};
	}

	public static <O> Source<O> nullSource() {
		return source(null);
	}

	public static <I> Sink<I> nullSink() {
		return new Sink<I>() {
			public void sink(I i) {
			}
		};
	}

	public static <O> Source<O> source(final O o) {
		return new Source<O>() {
			public O source() {
				return o;
			}
		};
	}

}
