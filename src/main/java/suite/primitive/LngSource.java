package suite.primitive;

import suite.adt.pair.LngObjPair;

public class LngSource {

	@FunctionalInterface
	public interface LngSource_ {
		public long source();
	}

	@FunctionalInterface
	public interface LngObjSource<T> {
		public boolean source2(LngObjPair<T> pair);
	}

}
