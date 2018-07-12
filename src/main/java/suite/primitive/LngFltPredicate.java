package suite.primitive;

import static suite.util.Friends.fail;

public interface LngFltPredicate {

	public boolean test(long c, float f);

	public default LngFltPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
