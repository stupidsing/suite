package suite.primitive;

import static primal.statics.Fail.fail;

public interface ChrIntPredicate {

	public boolean test(char c, int f);

	public default ChrIntPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
