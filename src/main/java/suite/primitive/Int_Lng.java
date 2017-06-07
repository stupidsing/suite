package suite.primitive;

@FunctionalInterface
public interface Int_Lng {

	public long apply(int c);

	public default Int_Lng rethrow() {
		return t -> {
			try {
				return apply(t);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + t, ex);
			}
		};
	}

}
