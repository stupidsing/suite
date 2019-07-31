package suite.primitive;

import static primal.statics.Fail.fail;

public interface DblDblPredicate {

	public boolean test(double c, double f);

	public default DblDblPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
