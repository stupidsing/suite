package suite.primitive;

public interface ChrFlt_Dbl {

	public double apply(char c, float f);

	public default ChrFlt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
