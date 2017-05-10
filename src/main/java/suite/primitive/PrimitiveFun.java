package suite.primitive;

import java.util.function.ToIntFunction;

public class PrimitiveFun {

	@FunctionalInterface
	public interface Double_Double {
		public double apply(double d);
	}

	@FunctionalInterface
	public interface Float_Float {
		public float apply(float f);
	}

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
	public interface IntObj_Double<T> {
		public double apply(int i, T t);
	}

	@FunctionalInterface
	public interface IntObj_Int<T> {
		public int apply(int i, T t);
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
	public interface ObjObj_Double<X, Y> {
		public double apply(X x, Y y);
	}

	@FunctionalInterface
	public interface ObjObj_Int<X, Y> {
		public int apply(X x, Y y);
	}

}
