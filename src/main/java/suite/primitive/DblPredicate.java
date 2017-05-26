package suite.primitive;

public class DblPredicate {

	@FunctionalInterface
	public interface DblPredicate_ {
		public boolean test(double c);
	}

	@FunctionalInterface
	public interface DblObjPredicate<T> {
		public boolean test(double c, T t);
	}

}
