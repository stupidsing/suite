package suite.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class IterUtil {

	private static class SourceIterator<T> implements Iterator<T> {
		private Source<T> source;
		private T next = null;

		private SourceIterator(Source<T> source) {
			this.source = source;
		}

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

	@SafeVarargs
	public static <T> Iterator<T> asIter(T... t) {
		return Arrays.asList(t).iterator();
	}

	public static <T> Iterator<T> cons(T t, Iterator<T> iter) {
		return concat(asIter(asIter(t), iter));
	}

	public static <T> Iterator<T> concat(final Iterator<Iterator<T>> iter) {
		return iter(new Source<T>() {
			private Iterator<T> iter0 = Collections.<T> emptySet().iterator();

			public T source() {
				while (!iter0.hasNext() && iter.hasNext())
					iter0 = iter.next();

				return iter0.hasNext() ? iter0.next() : null;
			}
		});
	}

	public static <T> Iterator<T> filter(final Fun<T, Boolean> fun, final Iterator<T> iter) {
		return iter(new Source<T>() {
			public T source() {
				while (iter.hasNext()) {
					T e = iter.next();

					if (fun.apply(e))
						return e;
				}

				return null;
			}
		});
	}

	public static <T, R> R fold(Fun<Pair<R, T>, R> fun, R init, Iterator<T> iter) {
		while (iter.hasNext())
			init = fun.apply(Pair.create(init, iter.next()));
		return init;
	}

	public static <T> Iterator<T> iter(Source<T> source) {
		return new SourceIterator<>(source);
	}

	public static <T> Iterable<T> iterable(Source<T> source) {
		return iterable(iter(source));
	}

	public static <T> Iterable<T> iterable(final Iterator<T> iter) {
		return new Iterable<T>() {
			public Iterator<T> iterator() {
				return iter;
			}
		};
	}

	public static <T0, T1> Iterator<T1> map(final Fun<T0, T1> fun, final Iterator<T0> iter) {
		return new Iterator<T1>() {
			public boolean hasNext() {
				return iter.hasNext();
			}

			public T1 next() {
				return fun.apply(iter.next());
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

}
