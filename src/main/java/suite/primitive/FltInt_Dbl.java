package suite.primitive;

public interface FltInt_Dbl {

	public double apply(float c, int f);

	public default FltInt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
