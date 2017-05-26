package suite.primitive;

import suite.adt.pair.DblObjPair;

public class DblSource {

	@FunctionalInterface
	public interface DblSource_ {
		public double source();
	}

	@FunctionalInterface
	public interface DblObjSource<T> {
		public boolean source2(DblObjPair<T> pair);
	}

}
