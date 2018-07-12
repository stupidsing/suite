package suite.primitive;

import static suite.util.Friends.fail;

public interface IntIntPredicate {

	public boolean test(int c, int f);

	public default IntIntPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
