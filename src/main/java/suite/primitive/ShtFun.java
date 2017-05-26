package suite.primitive;

public class ShtFun {

	@FunctionalInterface
	public interface ShtObj_Obj<X, Y> {
		public Y apply(short c, X x);
	}

	@FunctionalInterface
	public interface Sht_Obj<T> {
		T apply(short c);
	}

	@FunctionalInterface
	public interface Obj_Sht<T> {
		short apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Sht<X, Y> {
		short apply(X x, Y y);
	}

}
