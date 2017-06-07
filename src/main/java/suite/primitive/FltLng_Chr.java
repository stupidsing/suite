package suite.primitive;

public interface FltLng_Chr {

	public char apply(float c, long f);

	public default FltLng_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
