package suite.primitive;

public class LngPredicate {

	@FunctionalInterface
	public interface LngPredicate_ {
		public boolean test(long c);

		public default LngPredicate_ rethrow() {
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
	public interface LngObjPredicate<T> {
		public boolean test(long c, T t);

		public default LngObjPredicate<T> rethrow() {
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
