package suite.primitive;

public interface IntInt_Chr {

	public char apply(int c, int f);

	public default IntInt_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
