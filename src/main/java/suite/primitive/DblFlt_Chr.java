package suite.primitive;

public interface DblFlt_Chr {

	public char apply(double c, float f);

	public default DblFlt_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
