package suite.util;

import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

import suite.primitive.IntPrimitiveFun.IntObj_Obj;
import suite.primitive.IntPrimitiveFun.ObjObj_Int;
import suite.primitive.IntPrimitiveFun.Obj_Int;
import suite.primitive.PrimitiveFun.IntObj_Int;
import suite.primitive.PrimitiveFun.ObjObj_Obj;
import suite.primitive.PrimitivePredicate.IntObjPredicate;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;

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

	public static <I, O> Fun<I, O> fun(Fun<I, O> fun) {
		return i -> {
			try {
				return fun.apply(i);
			} catch (Exception ex) {
				throw new RuntimeException("for " + i, ex);
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

	public static <T> Obj_Int<T> fun(Obj_Int<T> fun) {
		return t -> {
			try {
				return fun.apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + t, ex);
			}
		};
	}

	public static <K, V> ObjObj_Int<K, V> fun2(ObjObj_Int<K, V> fun) {
		return (k, v) -> {
			try {
				return fun.apply(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static <K, V, T> ObjObj_Obj<K, V, T> fun2(ObjObj_Obj<K, V, T> fun) {
		return (k, v) -> {
			try {
				return fun.apply(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static <V> IntObjPredicate<V> intObjPredicate(IntObjPredicate<V> fun0) {
		return (k, v) -> {
			try {
				return fun0.test(k, v);
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

	public static <T> Predicate<T> predicate(Predicate<T> predicate) {
		return t -> {
			try {
				return predicate.test(t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + t, ex);
			}
		};
	}

	public static <T> Sink<T> sink(Sink<T> sink) {
		return t -> {
			try {
				sink.sink(t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + t, ex);
			}
		};
	}

}
