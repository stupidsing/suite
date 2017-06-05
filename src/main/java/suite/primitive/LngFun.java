package suite.primitive;

public class LngFun {

	@FunctionalInterface
	public interface LngObj_Obj<X, Y> {
		public Y apply(long c, X x);
	}

	@FunctionalInterface
	public interface Lng_Obj<T> {
		T apply(long c);
	}

	@FunctionalInterface
	public interface Obj_Lng<T> {
		long apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Lng<X, Y> {
		long apply(X x, Y y);
	}

}
