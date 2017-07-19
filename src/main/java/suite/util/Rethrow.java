package suite.util;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class Rethrow {

	public interface SourceEx<T, Ex extends Throwable> {
		public T source() throws Ex;
	}

	public static <K, V> BiConsumer<K, V> biConsumer(BiConsumer<K, V> fun0) {
		return (k, v) -> {
			try {
				fun0.accept(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static <K, V> BiPredicate<K, V> biPredicate(BiPredicate<K, V> fun0) {
		return (k, v) -> {
			try {
				return fun0.test(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static <T> T ex(SourceEx<T, Exception> source) {
		try {
			return source.source();
		} catch (Exception ex) {
			if (ex instanceof RuntimeException)
				throw (RuntimeException) ex;
			else
				throw new RuntimeException(ex);
		}
	}

	public static <T> Predicate<T> predicate(Predicate<T> predicate) {
		return t -> {
			try {
				return predicate.test(t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + t, ex);
			}
		};
	}

}
