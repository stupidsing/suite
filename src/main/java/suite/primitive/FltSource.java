package suite.primitive;

import suite.adt.pair.FltObjPair;

public class FltSource {

	@FunctionalInterface
	public interface FltSource_ {
		public float source();
	}

	@FunctionalInterface
	public interface FltObjSource<T> {
		public boolean source2(FltObjPair<T> pair);
	}

}
