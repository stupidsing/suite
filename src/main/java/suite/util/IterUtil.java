package suite.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import suite.util.FunUtil.Fun;

public class IterUtil {

	@SafeVarargs
	public static <T> Iterator<T> asIter(T... t) {
		return Arrays.asList(t).iterator();
	}

	public static <T> Iterator<T> cons(T t, final Iterator<T> iter) {
		return concat(asIter(asIter(t), iter));
	}

	public static <T> Iterator<T> concat(final Iterator<Iterator<T>> iter) {
		return new Iterator<T>() {
			private T next;
			private Iterator<T> iter0 = Collections.<T> emptySet().iterator();

			public boolean hasNext() {
				if (next == null) {
					while (!iter0.hasNext() && iter.hasNext())
						iter0 = iter.next();

					next = iter0.hasNext() ? iter0.next() : null;
				}

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

	public static <T, R> R fold(Fun<Pair<R, T>, R> fun, R init, Iterator<T> iter) {
		while (iter.hasNext())
			init = fun.apply(Pair.create(init, iter.next()));
		return init;
	}

}
