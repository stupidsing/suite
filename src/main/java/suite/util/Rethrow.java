package suite.util;

import static suite.util.Friends.fail;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public class Rethrow {

	public interface SinkEx<T, Ex extends Exception> {
		public void f(T t) throws Ex;
	}

	public interface SourceEx<T, Ex extends Exception> {
		public T g() throws Ex;
	}

	public static <K, V> BiPredicate<K, V> biPredicate(BiPredicate<K, V> fun0) {
		return (k, v) -> {
			try {
				return fun0.test(k, v);
			} catch (Exception ex) {
				return fail("for key " + k, ex);
			}
		};
	}

	public static <T> T ex(SourceEx<T, Exception> source) {
		try {
			return source.g();
		} catch (Exception ex) {
			return fail(ex);
		}
	}

	public static <T> Predicate<T> predicate(Predicate<T> predicate) {
		return t -> {
			try {
				return predicate.test(t);
			} catch (Exception ex) {
				return fail("for " + t, ex);
			}
		};
	}

}
