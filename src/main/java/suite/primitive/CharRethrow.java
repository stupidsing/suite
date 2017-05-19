package suite.primitive;

import suite.primitive.CharPrimitiveFun.CharObj_Char;
import suite.primitive.CharPrimitiveFun.CharObj_Obj;
import suite.primitive.CharPrimitiveFun.ObjObj_Char;
import suite.primitive.CharPrimitiveFun.Obj_Char;
import suite.primitive.CharPrimitivePredicate.CharObjPredicate;
import suite.primitive.CharPrimitivePredicate.CharPredicate_;

public class CharRethrow {

	public static <V> CharObj_Char<V> fun2(CharObj_Char<V> fun) {
		return (k, v) -> {
			try {
				return fun.apply(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static <V, T> CharObj_Obj<V, T> fun2(CharObj_Obj<V, T> fun) {
		return (k, v) -> {
			try {
				return fun.apply(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static <T> Obj_Char<T> fun(Obj_Char<T> fun) {
		return t -> {
			try {
				return fun.apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + t, ex);
			}
		};
	}

	public static <K, V> ObjObj_Char<K, V> fun2(ObjObj_Char<K, V> fun) {
		return (k, v) -> {
			try {
				return fun.apply(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static <V> CharObjPredicate<V> charObjPredicate(CharObjPredicate<V> fun0) {
		return (k, v) -> {
			try {
				return fun0.test(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

	public static CharPredicate_ predicate(CharPredicate_ predicate) {
		return t -> {
			try {
				return predicate.test(t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + t, ex);
			}
		};
	}

}
