package suite.primitive; import static suite.util.Friends.fail;

public interface FltFltPredicate {

	public boolean test(float c, float f);

	public default FltFltPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
