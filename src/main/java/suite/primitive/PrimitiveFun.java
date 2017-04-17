package suite.primitive;

import java.util.function.ToIntFunction;

public class PrimitiveFun {

	@FunctionalInterface
	public interface IntFloatFun {
		public float apply(int i);
	}

	@FunctionalInterface
	public interface IntIntFun {
		public int apply(int i);
	}

	@FunctionalInterface
	public interface IntIntFloatFun {
		public float apply(int i, int j);
	}

	@FunctionalInterface
	public interface IntObjFun<T> {
		public T apply(int i);
	}

	@FunctionalInterface
	public interface IntSource {
		public int source();
	}

	@FunctionalInterface
	public interface ObjIntFun<T> extends ToIntFunction<T> {
	}

}
