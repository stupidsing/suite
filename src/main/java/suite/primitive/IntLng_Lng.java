package suite.primitive;

public interface IntLng_Lng {

	public long apply(int c, long f);

	public default IntLng_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
