package suite.primitive;

public interface DblFlt_Lng {

	public long apply(double c, float f);

	public default DblFlt_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
