package suite.primitive;

@FunctionalInterface
public interface Lng_Chr {

	public char apply(long c);

	public default Lng_Chr rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
