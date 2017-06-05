package suite.primitive;

public interface IntLng_Dbl {

	public double apply(int c, long f);

	public default IntLng_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
