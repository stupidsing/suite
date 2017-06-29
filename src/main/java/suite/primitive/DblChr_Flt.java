package suite.primitive;

public interface DblChr_Flt {

	public float apply(double c, char f);

	public default DblChr_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f, ex);
			}
		};

	}
}
