package suite.primitive;

import static primal.statics.Fail.fail;

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
