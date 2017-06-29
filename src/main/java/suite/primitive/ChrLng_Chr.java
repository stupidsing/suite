package suite.primitive;

public interface ChrLng_Chr {

	public char apply(char c, long f);

	public default ChrLng_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
