package suite.primitive;

public class IntPrimitivePredicate {

	@FunctionalInterface
	public interface IntObjPredicate<T> {
		public boolean test(int i, T t);
	}

}
