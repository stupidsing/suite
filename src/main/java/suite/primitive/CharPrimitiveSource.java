package suite.primitive;

import suite.adt.pair.CharCharPair;
import suite.adt.pair.CharObjPair;

public class CharPrimitiveSource {

	@FunctionalInterface
	public interface CharSource {
		public char source();
	}

	@FunctionalInterface
	public interface CharCharSource {
		public boolean source2(CharCharPair pair);
	}

	@FunctionalInterface
	public interface CharObjSource<T> {
		public boolean source2(CharObjPair<T> pair);
	}

}
