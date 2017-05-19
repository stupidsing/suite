package suite.primitive;

public class CharPrimitivePredicate {

	@FunctionalInterface
	public interface CharPredicate_ {
		public boolean test(char c);
	}

	@FunctionalInterface
	public interface CharObjPredicate<T> {
		public boolean test(char c, T t);
	}

}
