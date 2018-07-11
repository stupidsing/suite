package suite.primitive; import static suite.util.Friends.fail;

public interface DblIntPredicate {

	public boolean test(double c, int f);

	public default DblIntPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
