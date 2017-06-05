package suite.primitive;

public interface FltObj_Dbl<T> {

	public double apply(float c, T t);

	public default FltObj_Dbl<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
