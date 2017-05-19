package suite.primitive;

public class IntPrimitivePredicate {

	@FunctionalInterface
	public interface IntPredicate_ {
		public boolean test(int c);
	}

	@FunctionalInterface
	public interface IntObjPredicate<T> {
		public boolean test(int c, T t);
	}

}
