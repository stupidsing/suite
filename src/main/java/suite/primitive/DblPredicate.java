package suite.primitive;

public class DblPredicate {

	@FunctionalInterface
	public interface DblPredicate_ {
		public boolean test(double c);

		public default DblPredicate_ rethrow() {
			return c -> {
				try {
					return test(c);
				} catch (Exception ex) {
					throw new RuntimeException("for " + c + ", ", ex);
				}
			};
		}
	}

	@FunctionalInterface
	public interface DblObjPredicate<T> {
		public boolean test(double c, T t);

		public default DblObjPredicate<T> rethrow() {
			return (c, t) -> {
				try {
					return test(c, t);
				} catch (Exception ex) {
					throw new RuntimeException("for " + c + ":" + t + ", ", ex);
				}
			};
		}
	}

}
