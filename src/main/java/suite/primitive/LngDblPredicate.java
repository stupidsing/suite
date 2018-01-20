package suite.primitive;

import suite.util.Fail;

public interface LngDblPredicate {

	public boolean test(long c, double f);

	public default LngDblPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
