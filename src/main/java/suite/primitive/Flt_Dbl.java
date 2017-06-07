package suite.primitive;

@FunctionalInterface
public interface Flt_Dbl {

	public double apply(float c);

	public default Flt_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
