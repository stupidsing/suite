package suite.primitive;

import java.util.function.BiFunction;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;

public class PrimitiveFun {

	@FunctionalInterface
	public interface Double_Double {
		public double apply(double d);
	}

	@FunctionalInterface
	public interface Int_Double {
		public double apply(int i);
	}

	@FunctionalInterface
	public interface IntObj_Double<T> {
		public double apply(int i, T t);
	}

	@FunctionalInterface
	public interface Obj_Double<T> extends ToDoubleFunction<T> {
	}

	@FunctionalInterface
	public interface ObjObj_Double<X, Y> extends ToDoubleBiFunction<X, Y> {
	}

	@FunctionalInterface
	public interface ObjObj_Obj<X, Y, Z> extends BiFunction<X, Y, Z> {
	}

}
