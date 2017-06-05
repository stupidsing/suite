package suite.primitive;

public interface DblInt_Lng {

	public long apply(double c, int f);

	public default DblInt_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
