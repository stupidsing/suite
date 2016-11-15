package suite.util;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import suite.util.FunUtil.Fun;

public class Rethrow {

	public interface SourceEx<T, Ex extends Throwable> {
		public T source() throws Ex;
	}

	public interface SourceReflectiveOperationException<T> {
		public T source() throws ReflectiveOperationException;
	}

	public static <V, K> BiPredicate<K, V> bipredicate(BiPredicate<K, V> fun0) {
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
			throw new RuntimeException(ex);
		}
	}

	public static <I, O> Fun<I, O> fun(Fun<I, O> fun) {
		return i -> {
			try {
				return fun.apply(i);
			} catch (Exception ex) {
				throw new RuntimeException("for " + i, ex);
			}
		};
	}

	public static <K, V, T> BiFunction<K, V, T> fun2(BiFunction<K, V, T> fun) {
		return (k, v) -> {
			try {
				return fun.apply(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static <T> T ioException(SourceEx<T, IOException> source) {
		try {
			return source.source();
		} catch (IOException ex) {
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

	public static <T> T reflectiveOperationException(SourceReflectiveOperationException<T> source) {
		try {
			return source.source();
		} catch (ReflectiveOperationException ex) {
			throw new RuntimeException(ex);
		}
	}

}
