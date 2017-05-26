package suite.primitive;

import suite.adt.pair.ShtObjPair;

public class ShtPrimitiveSource {

	@FunctionalInterface
	public interface ShtSource {
		public short source();
	}

	@FunctionalInterface
	public interface ShtObjSource<T> {
		public boolean source2(ShtObjPair<T> pair);
	}

}
