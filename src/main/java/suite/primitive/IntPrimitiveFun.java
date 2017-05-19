package suite.primitive;

public class IntPrimitiveFun {

	@FunctionalInterface
	public interface Int_Int {
		public int apply(int i);
	}

	@FunctionalInterface
	public interface IntInt_Obj<T> {
		public T apply(int i, int j);
	}

	@FunctionalInterface
	public interface IntObj_Obj<X, Y> {
		public Y apply(int i, X x);
	}

	@FunctionalInterface
	public interface Int_Obj<T> {
		T apply(int c);
	}

	@FunctionalInterface
	public interface Obj_Int<T> {
		int apply(T t);
	}

	@FunctionalInterface
	public interface ObjObj_Int<X, Y> {
		int apply(X x, Y y);
	}

}
