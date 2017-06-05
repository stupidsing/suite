package suite.primitive;

public class LngPredicate {

	@FunctionalInterface
	public interface LngPredicate_ {
		public boolean test(long c);
	}

	@FunctionalInterface
	public interface LngObjPredicate<T> {
		public boolean test(long c, T t);
	}

}
