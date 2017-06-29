package suite.primitive;

public interface LngInt_Dbl {

	public double apply(long c, int f);

	public default LngInt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
