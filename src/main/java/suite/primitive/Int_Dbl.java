package suite.primitive;

@FunctionalInterface
public interface Int_Dbl {

	public double apply(int c);

	public default Int_Dbl rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
