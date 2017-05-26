package suite.primitive;

import suite.adt.pair.IntObjPair;

public class IntPrimitiveSource {

	@FunctionalInterface
	public interface IntSource {
		public int source();
	}

	@FunctionalInterface
	public interface IntObjSource<T> {
		public boolean source2(IntObjPair<T> pair);
	}

}
