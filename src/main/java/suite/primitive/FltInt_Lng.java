package suite.primitive;

public interface FltInt_Lng {

	public long apply(float c, int f);

	public default FltInt_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
