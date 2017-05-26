package suite.primitive;

public class ChrPredicate {

	@FunctionalInterface
	public interface ChrPredicate_ {
		public boolean test(char c);
	}

	@FunctionalInterface
	public interface ChrObjPredicate<T> {
		public boolean test(char c, T t);
	}

}
