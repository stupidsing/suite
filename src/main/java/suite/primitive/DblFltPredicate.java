package suite.primitive;

import static suite.util.Fail.fail;

public interface DblFltPredicate {

	public boolean test(double c, float f);

	public default DblFltPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
