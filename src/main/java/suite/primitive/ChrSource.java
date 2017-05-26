package suite.primitive;

import suite.adt.pair.ChrObjPair;

public class ChrSource {

	@FunctionalInterface
	public interface ChrSource_ {
		public char source();
	}

	@FunctionalInterface
	public interface ChrObjSource<T> {
		public boolean source2(ChrObjPair<T> pair);
	}

}
