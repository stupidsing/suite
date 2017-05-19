package suite.primitive;

import suite.primitive.IntPrimitiveFun.IntObj_Int;
import suite.primitive.IntPrimitiveFun.IntObj_Obj;
import suite.primitive.IntPrimitiveFun.ObjObj_Int;
import suite.primitive.IntPrimitiveFun.Obj_Int;
import suite.primitive.IntPrimitivePredicate.IntObjPredicate;
import suite.primitive.IntPrimitivePredicate.IntPredicate_;

public class IntRethrow {

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

	public static <V> IntObjPredicate<V> intObjPredicate(IntObjPredicate<V> fun0) {
		return (k, v) -> {
			try {
				return fun0.test(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static IntPredicate_ predicate(IntPredicate_ predicate) {
		return t -> {
			try {
				return predicate.test(t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + t, ex);
			}
		};
	}

}
