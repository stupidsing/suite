package suite.primitive;

public interface IntLng_Chr {

	public char apply(int c, long f);

	public default IntLng_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
