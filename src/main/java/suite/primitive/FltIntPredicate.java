package suite.primitive;

import suite.util.Fail;

public interface FltIntPredicate {

	public boolean test(float c, int f);

	public default FltIntPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
