package suite.primitive;

@FunctionalInterface
public interface Lng_Lng {

	public long apply(long c);

	public default Lng_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
