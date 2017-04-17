package suite.util;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import suite.primitive.PrimitiveFun.IntObj_Int;
import suite.primitive.PrimitiveFun.IntObj_Obj;
import suite.primitive.PrimitivePredicate.IntObjPredicate;
import suite.util.FunUtil.Fun;

public class Rethrow {

	public interface SourceEx<T, Ex extends Throwable> {
		public T source() throws Ex;
	}

	public static <K, V> BiPredicate<K, V> bipredicate(BiPredicate<K, V> fun0) {
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

	public static <V> IntObj_Int<V> fun2(IntObj_Int<V> fun) {
		return (k, v) -> {
			try {
				return fun.apply(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static <V, T> IntObj_Obj<V, T> fun2(IntObj_Obj<V, T> fun) {
		return (k, v) -> {
			try {
				return fun.apply(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static IntPredicate predicate(IntPredicate predicate) {
		return t -> {
			try {
				return predicate.test(t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + t, ex);
			}
		};
	}

	public static <V> IntObjPredicate<V> bipredicate(IntObjPredicate<V> fun0) {
		return (k, v) -> {
			try {
				return fun0.test(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
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
