package suite.primitive;

import suite.adt.IntIntPair;
import suite.adt.IntObjPair;

public class PrimitiveSource {

	@FunctionalInterface
	public interface IntSource {
		public int source();
	}

	@FunctionalInterface
	public interface IntIntSource2 {
		public boolean source2(IntIntPair pair);
	}

	@FunctionalInterface
	public interface IntObjSource2<T> {
		public boolean source2(IntObjPair<T> pair);
	}

}
