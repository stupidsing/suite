package suite.primitive;

import suite.adt.IntIntPair;
import suite.adt.IntObjPair;

public class PrimitiveSource {

	@FunctionalInterface
	public interface IntSource {
		public int source();
	}

	@FunctionalInterface
	public interface IntIntSource {
		public boolean source(IntIntPair pair);
	}

	@FunctionalInterface
	public interface IntObjSource<T> {
		public boolean source2(IntObjPair<T> pair);
	}

}
