package suite.primitive; import static suite.util.Friends.fail;

public interface LngLngPredicate {

	public boolean test(long c, long f);

	public default LngLngPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
