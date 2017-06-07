package suite.primitive;

@FunctionalInterface
public interface Flt_Int {

	public int apply(float c);

	public default Flt_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
