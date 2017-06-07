package suite.primitive;

@FunctionalInterface
public interface Lng_Int {

	public int apply(long c);

	public default Lng_Int rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
