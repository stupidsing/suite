package suite.primitive; import static suite.util.Friends.fail;

public interface LngFlt_Obj<T> {

	public T apply(long c, float f);

	public default LngFlt_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
