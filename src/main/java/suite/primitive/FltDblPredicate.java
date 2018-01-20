package suite.primitive;

import suite.util.Fail;

public interface FltDblPredicate {

	public boolean test(float c, double f);

	public default FltDblPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
