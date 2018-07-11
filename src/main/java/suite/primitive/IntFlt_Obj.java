package suite.primitive; import static suite.util.Friends.fail;

public interface IntFlt_Obj<T> {

	public T apply(int c, float f);

	public default IntFlt_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
