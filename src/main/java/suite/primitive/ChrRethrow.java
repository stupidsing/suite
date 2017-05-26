package suite.primitive;

import suite.primitive.ChrPrimitiveFun.ChrObj_Chr;
import suite.primitive.ChrPrimitiveFun.ChrObj_Obj;
import suite.primitive.ChrPrimitiveFun.ObjObj_Chr;
import suite.primitive.ChrPrimitiveFun.Obj_Chr;
import suite.primitive.ChrPrimitivePredicate.ChrObjPredicate;
import suite.primitive.ChrPrimitivePredicate.ChrPredicate_;

public class ChrRethrow {

	public static <V> ChrObj_Chr<V> fun2(ChrObj_Chr<V> fun) {
		return (k, v) -> {
			try {
				return fun.apply(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static <V, T> ChrObj_Obj<V, T> fun2(ChrObj_Obj<V, T> fun) {
		return (k, v) -> {
			try {
				return fun.apply(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static <T> Obj_Chr<T> fun(Obj_Chr<T> fun) {
		return t -> {
			try {
				return fun.apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + t, ex);
			}
		};
	}

	public static <K, V> ObjObj_Chr<K, V> fun2(ObjObj_Chr<K, V> fun) {
		return (k, v) -> {
			try {
				return fun.apply(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static <V> ChrObjPredicate<V> charObjPredicate(ChrObjPredicate<V> fun0) {
		return (k, v) -> {
			try {
				return fun0.test(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static ChrPredicate_ predicate(ChrPredicate_ predicate) {
		return t -> {
			try {
				return predicate.test(t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + t, ex);
			}
		};
	}

}
