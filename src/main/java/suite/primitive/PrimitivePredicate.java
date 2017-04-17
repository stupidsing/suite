package suite.primitive;

public class PrimitivePredicate {

	@FunctionalInterface
	public interface IntObjPredicate<T> {
		public boolean test(int i, T t);
	}

}
