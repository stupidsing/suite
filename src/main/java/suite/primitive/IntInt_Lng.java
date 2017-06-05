package suite.primitive;

public interface IntInt_Lng {

	public long apply(int c, int f);

	public default IntInt_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
