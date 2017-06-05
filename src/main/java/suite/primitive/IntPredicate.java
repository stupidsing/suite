package suite.primitive;

public class IntPredicate {

	@FunctionalInterface
	public interface IntPredicate_ {
		public boolean test(int c);

		public default IntPredicate_ rethrow() {
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
	public interface IntObjPredicate<T> {
		public boolean test(int c, T t);

		public default IntObjPredicate<T> rethrow() {
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
