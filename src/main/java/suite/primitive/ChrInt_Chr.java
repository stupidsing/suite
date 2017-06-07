package suite.primitive;

public interface ChrInt_Chr {

	public char apply(char c, int f);

	public default ChrInt_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
