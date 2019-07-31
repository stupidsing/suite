package suite.primitive;

import static primal.statics.Fail.fail;

public interface FltLngPredicate {

	public boolean test(float c, long f);

	public default FltLngPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
