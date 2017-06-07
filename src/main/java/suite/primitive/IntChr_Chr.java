package suite.primitive;

public interface IntChr_Chr {

	public char apply(int c, char f);

	public default IntChr_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
