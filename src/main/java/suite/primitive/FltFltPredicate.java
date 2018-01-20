package suite.primitive;

import suite.util.Fail;

public interface FltFltPredicate {

	public boolean test(float c, float f);

	public default FltFltPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
