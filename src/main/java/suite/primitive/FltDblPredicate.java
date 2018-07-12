package suite.primitive;

import static suite.util.Friends.fail;

public interface FltDblPredicate {

	public boolean test(float c, double f);

	public default FltDblPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
