package suite.primitive;

public interface FltInt_Obj<T> {

	public T apply(float c, int f);

	public default FltInt_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
