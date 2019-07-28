package suite.primitive;

import static suite.util.Fail.fail;

public interface ChrLngPredicate {

	public boolean test(char c, long f);

	public default ChrLngPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
