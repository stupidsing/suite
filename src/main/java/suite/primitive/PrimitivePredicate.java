package suite.primitive;

public class PrimitivePredicate {

	@FunctionalInterface
	public interface IntObjPredicate2<T> {
		public boolean test(int i, T t);
	}

}
