package suite.primitive; import static suite.util.Friends.fail;

public interface IntFlt_Int {

	public int apply(int c, float f);

	public default IntFlt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
