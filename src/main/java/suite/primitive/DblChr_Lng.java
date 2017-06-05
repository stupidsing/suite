package suite.primitive;

public interface DblChr_Lng {

	public long apply(double c, char f);

	public default DblChr_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
