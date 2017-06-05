package suite.primitive;

public interface IntChr_Lng {

	public long apply(int c, char f);

	public default IntChr_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
