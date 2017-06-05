package suite.primitive;

public interface FltChr_Obj<T> {

	public T apply(float c, char f);

	public default FltChr_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
