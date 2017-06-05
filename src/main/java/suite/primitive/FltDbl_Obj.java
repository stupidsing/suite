package suite.primitive;

public interface FltDbl_Obj<T> {

	public T apply(float c, double f);

	public default FltDbl_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
