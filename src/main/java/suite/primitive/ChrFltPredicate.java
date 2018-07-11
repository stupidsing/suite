package suite.primitive; import static suite.util.Friends.fail;

public interface ChrFltPredicate {

	public boolean test(char c, float f);

	public default ChrFltPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
