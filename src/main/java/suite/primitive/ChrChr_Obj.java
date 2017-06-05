package suite.primitive;

public interface ChrChr_Obj<T> {

	public T apply(char c, char f);

	public default ChrChr_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
