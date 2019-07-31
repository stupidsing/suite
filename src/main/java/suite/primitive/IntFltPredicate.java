package suite.primitive;

import static primal.statics.Fail.fail;

public interface IntFltPredicate {

	public boolean test(int c, float f);

	public default IntFltPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
