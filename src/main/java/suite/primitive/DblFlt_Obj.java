package suite.primitive;

public interface DblFlt_Obj<T> {

	public T apply(double c, float f);

	public default DblFlt_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
