package suite.primitive;

import suite.adt.IntIntPair;
import suite.adt.IntObjPair;
import suite.adt.ObjIntPair;

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

	@FunctionalInterface
	public interface ObjIntSource2<T> {
		public boolean source2(ObjIntPair<T> pair);
	}

}
