package suite.primitive;

import java.util.function.ToIntFunction;

import suite.adt.IntIntPair;
import suite.adt.IntObjPair;
import suite.adt.ObjIntPair;

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

	@FunctionalInterface
	public interface Sink2_IntInt {
		public void sink2(int i, int j);
	}

	@FunctionalInterface
	public interface Sink2_IntObj<T> {
		public void sink2(int i, T t);
	}

	@FunctionalInterface
	public interface Sink2_ObjInt<T> {
		public void sink2(T t, int i);
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

	@FunctionalInterface
	public interface Source2_ObjInt<T> {
		public boolean source2(ObjIntPair<T> pair);
	}

}
