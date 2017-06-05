package suite.primitive;

public interface IntFlt_Obj<T> {

	public T apply(int c, float f);

	public default IntFlt_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
