package suite.primitive;

import java.util.function.ToIntFunction;

public class PrimitiveFun {

	@FunctionalInterface
	public interface Int_Float {
		public float apply(int i);
	}

	@FunctionalInterface
	public interface Int_Int {
		public int apply(int i);
	}

	@FunctionalInterface
	public interface IntInt_Float {
		public float apply(int i, int j);
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
		public T apply(int i);
	}

	@FunctionalInterface
	public interface Obj_Int<T> extends ToIntFunction<T> {
	}

	@FunctionalInterface
	public interface ObjInt_Obj<X, Y> {
		public Y apply(X x, int i);
	}

}
