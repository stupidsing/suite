package suite.primitive;

public interface IntObj_Int<T> {

	public int apply(int c, T t);

	public default IntObj_Int<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
