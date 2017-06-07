package suite.primitive;

@FunctionalInterface
public interface Lng_Dbl {

	public double apply(long c);

	public default Lng_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
