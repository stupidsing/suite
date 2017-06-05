package suite.primitive;

public interface DblDbl_Obj<T> {

	public T apply(double c, double f);

	public default DblDbl_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
