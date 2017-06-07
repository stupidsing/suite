package suite.primitive;

public interface IntInt_Flt {

	public float apply(int c, int f);

	public default IntInt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
