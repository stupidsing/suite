package suite.primitive;

public class ChrFun {

	@FunctionalInterface
	public interface ChrObj_Obj<X, Y> {
		public Y apply(char c, X x);
	}

	@FunctionalInterface
	public interface Chr_Obj<T> {
		T apply(char c);
	}

	@FunctionalInterface
	public interface Obj_Chr<T> {
		char apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Chr<X, Y> {
		char apply(X x, Y y);
	}

}
