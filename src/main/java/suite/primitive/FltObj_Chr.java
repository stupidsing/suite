package suite.primitive;

public interface FltObj_Chr<T> {

	public char apply(float c, T t);

	public default FltObj_Chr<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
