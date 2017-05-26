package suite.primitive;

import suite.adt.pair.FltObjPair;

public class FltPrimitiveSource {

	@FunctionalInterface
	public interface FltSource {
		public float source();
	}

	@FunctionalInterface
	public interface FltObjSource<T> {
		public boolean source2(FltObjPair<T> pair);
	}

}
