package suite.primitive;

import java.util.function.ToIntFunction;

import suite.adt.IntIntPair;
import suite.adt.IntObjPair;

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
	public interface Int_Obj<T> {
		public T apply(int i);
	}

	@FunctionalInterface
	public interface Obj_Int<T> extends ToIntFunction<T> {
	}

	@FunctionalInterface
	public interface Source_Int {
		public int source();
	}

	@FunctionalInterface
	public interface Source2_IntInt {
		public boolean source2(IntIntPair pair);
	}

	@FunctionalInterface
	public interface Source2_IntObj<T> {
		public boolean source2(IntObjPair<T> pair);
	}

}
