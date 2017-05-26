package suite.primitive;

import java.util.function.BiFunction;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;

public class PrimitiveFun {

	@FunctionalInterface
	public interface Dbl_Dbl {
		public double apply(double d);
	}

	@FunctionalInterface
	public interface Int_Dbl {
		public double apply(int i);
	}

	@FunctionalInterface
	public interface IntObj_Dbl<T> {
		public double apply(int i, T t);
	}

	@FunctionalInterface
	public interface Obj_Dbl<T> extends ToDoubleFunction<T> {
	}

	@FunctionalInterface
	public interface ObjObj_Dbl<X, Y> extends ToDoubleBiFunction<X, Y> {
	}

	@FunctionalInterface
	public interface ObjObj_Obj<X, Y, Z> extends BiFunction<X, Y, Z> {
	}

}
