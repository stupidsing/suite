package suite.primitive;

public interface ChrObj_Int<T> {

	public int apply(char c, T t);

	public default ChrObj_Int<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
