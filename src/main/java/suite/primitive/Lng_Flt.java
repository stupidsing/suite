package suite.primitive;

@FunctionalInterface
public interface Lng_Flt {

	public float apply(long c);

	public default Lng_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
