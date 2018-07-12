package suite.primitive;

import static suite.util.Friends.fail;

public interface IntChrPredicate {

	public boolean test(int c, char f);

	public default IntChrPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
