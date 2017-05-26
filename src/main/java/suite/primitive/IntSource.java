package suite.primitive;

import suite.adt.pair.IntObjPair;

public class IntSource {

	@FunctionalInterface
	public interface IntSource_ {
		public int source();
	}

	@FunctionalInterface
	public interface IntObjSource<T> {
		public boolean source2(IntObjPair<T> pair);
	}

}
