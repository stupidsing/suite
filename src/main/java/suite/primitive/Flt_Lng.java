package suite.primitive;

@FunctionalInterface
public interface Flt_Lng {

	public long apply(float c);

	public default Flt_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
