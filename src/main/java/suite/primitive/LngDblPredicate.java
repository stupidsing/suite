package suite.primitive;

import static primal.statics.Fail.fail;

public interface LngDblPredicate {

	public boolean test(long c, double f);

	public default LngDblPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
