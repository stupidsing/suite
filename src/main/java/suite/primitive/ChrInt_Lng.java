package suite.primitive;

public interface ChrInt_Lng {

	public long apply(char c, int f);

	public default ChrInt_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
