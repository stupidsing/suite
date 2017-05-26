package suite.primitive;

import suite.adt.pair.ShtObjPair;

public class ShtSource {

	@FunctionalInterface
	public interface ShtSource_ {
		public short source();
	}

	@FunctionalInterface
	public interface ShtObjSource<T> {
		public boolean source2(ShtObjPair<T> pair);
	}

}
