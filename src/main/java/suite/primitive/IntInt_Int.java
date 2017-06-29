package suite.primitive;

public interface IntInt_Int {

	public int apply(int c, int f);

	public default IntInt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
