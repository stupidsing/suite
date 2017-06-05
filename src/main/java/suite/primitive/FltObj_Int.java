package suite.primitive;

public interface FltObj_Int<T> {

	public int apply(float c, T t);

	public default FltObj_Int<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
