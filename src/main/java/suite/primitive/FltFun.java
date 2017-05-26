package suite.primitive;

public class FltFun {

	@FunctionalInterface
	public interface FltObj_Obj<X, Y> {
		public Y apply(float c, X x);
	}

	@FunctionalInterface
	public interface Flt_Obj<T> {
		T apply(float c);
	}

	@FunctionalInterface
	public interface Obj_Flt<T> {
		float apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Flt<X, Y> {
		float apply(X x, Y y);
	}

}
