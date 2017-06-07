package suite.primitive;

public interface DblLng_Chr {

	public char apply(double c, long f);

	public default DblLng_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
