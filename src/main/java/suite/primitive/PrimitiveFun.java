package suite.primitive;

import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

public class PrimitiveFun {

	@FunctionalInterface
	public interface Char_Char {
		public char apply(char d);
	}

	@FunctionalInterface
	public interface Double_Double {
		public double apply(double d);
	}

	@FunctionalInterface
	public interface Float_Float {
		public float apply(float f);
	}

	@FunctionalInterface
	public interface Int_Double {
		public double apply(int i);
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
	public interface IntObj_Float<T> {
		public float apply(int i, T t);
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
	public interface Int_Obj<T> extends IntFunction<T> {
	}

	@FunctionalInterface
	public interface Obj_Double<T> extends ToDoubleFunction<T> {
	}

	@FunctionalInterface
	public interface Obj_Float<T> {
		public float applyAsFloat(T t);
	}

	@FunctionalInterface
	public interface Obj_Int<T> extends ToIntFunction<T> {
	}

	@FunctionalInterface
	public interface ObjObj_Double<X, Y> extends ToDoubleBiFunction<X, Y> {
	}

	@FunctionalInterface
	public interface ObjObj_Float<X, Y> {
		public float applyAsFloat(X x, Y y);
	}

	@FunctionalInterface
	public interface ObjObj_Int<X, Y> extends ToIntBiFunction<X, Y> {
	}

	@FunctionalInterface
	public interface ObjObj_Obj<X, Y, Z> extends BiFunction<X, Y, Z> {
	}

}
