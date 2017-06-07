package suite.primitive;

@FunctionalInterface
public interface Dbl_Lng {

	public long apply(double c);

	public default Dbl_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
