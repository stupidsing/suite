package suite.primitive;

import static primal.statics.Fail.fail;

public interface DblLngPredicate {

	public boolean test(double c, long f);

	public default DblLngPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
