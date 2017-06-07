package suite.primitive;

@FunctionalInterface
public interface Dbl_Int {

	public int apply(double c);

	public default Dbl_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
