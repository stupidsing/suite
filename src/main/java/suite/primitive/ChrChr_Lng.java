package suite.primitive;

public interface ChrChr_Lng {

	public long apply(char c, char f);

	public default ChrChr_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
