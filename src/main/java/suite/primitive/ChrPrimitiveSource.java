package suite.primitive;

import suite.adt.pair.ChrObjPair;

public class ChrPrimitiveSource {

	@FunctionalInterface
	public interface ChrSource {
		public char source();
	}

	@FunctionalInterface
	public interface ChrObjSource<T> {
		public boolean source2(ChrObjPair<T> pair);
	}

}
