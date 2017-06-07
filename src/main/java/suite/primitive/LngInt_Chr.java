package suite.primitive;

public interface LngInt_Chr {

	public char apply(long c, int f);

	public default LngInt_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
