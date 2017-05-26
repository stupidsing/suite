package suite.primitive;

public class FltPredicate {

	@FunctionalInterface
	public interface FltPredicate_ {
		public boolean test(float c);
	}

	@FunctionalInterface
	public interface FltObjPredicate<T> {
		public boolean test(float c, T t);
	}

}
