package suite.primitive;

public interface ChrObj_Dbl<T> {

	public double apply(char c, T t);

	public default ChrObj_Dbl<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
