package suite.primitive;

public interface DblLng_Dbl {

	public double apply(double c, long f);

	public default DblLng_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
