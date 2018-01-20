package suite.primitive;

import suite.util.Fail;

public interface IntDblPredicate {

	public boolean test(int c, double f);

	public default IntDblPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
