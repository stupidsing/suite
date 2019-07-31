package suite.primitive;

import static primal.statics.Fail.fail;

public interface LngIntPredicate {

	public boolean test(long c, int f);

	public default LngIntPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
