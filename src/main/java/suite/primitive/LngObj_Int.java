package suite.primitive;

public interface LngObj_Int<T> {

	public int apply(long c, T t);

	public default LngObj_Int<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
