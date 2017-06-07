package suite.primitive;

public interface DblFlt_Dbl {

	public double apply(double c, float f);

	public default DblFlt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
