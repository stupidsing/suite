package suite.primitive;

public interface ChrLng_Dbl {

	public double apply(char c, long f);

	public default ChrLng_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
