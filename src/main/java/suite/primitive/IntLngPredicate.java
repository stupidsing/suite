package suite.primitive; import static suite.util.Friends.fail;

public interface IntLngPredicate {

	public boolean test(int c, long f);

	public default IntLngPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
