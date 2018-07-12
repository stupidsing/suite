package suite.primitive;

import static suite.util.Friends.fail;

public interface FltChrPredicate {

	public boolean test(float c, char f);

	public default FltChrPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
