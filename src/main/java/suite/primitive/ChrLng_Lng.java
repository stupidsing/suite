package suite.primitive;

public interface ChrLng_Lng {

	public long apply(char c, long f);

	public default ChrLng_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
