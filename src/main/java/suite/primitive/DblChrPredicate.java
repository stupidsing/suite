package suite.primitive; import static suite.util.Friends.fail;

public interface DblChrPredicate {

	public boolean test(double c, char f);

	public default DblChrPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
