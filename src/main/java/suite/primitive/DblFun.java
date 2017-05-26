package suite.primitive;

public class DblFun {

	@FunctionalInterface
	public interface DblObj_Obj<X, Y> {
		public Y apply(double c, X x);
	}

	@FunctionalInterface
	public interface Dbl_Obj<T> {
		T apply(double c);
	}

	@FunctionalInterface
	public interface Obj_Dbl<T> {
		double apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Dbl<X, Y> {
		double apply(X x, Y y);
	}

}
