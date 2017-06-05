package suite.primitive;

public interface ChrFlt_Chr {

	public char apply(char c, float f);

	public default ChrFlt_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};

	}
}
