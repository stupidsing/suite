package suite.primitive;

@FunctionalInterface
public interface Int_Flt {

	public float apply(int c);

	public default Int_Flt rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
