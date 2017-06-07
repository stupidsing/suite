package suite.primitive;

public interface DblFlt_Flt {

	public float apply(double c, float f);

	public default DblFlt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ", " + f, ex);
			}
		};

	}
}
