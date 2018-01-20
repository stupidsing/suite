package suite.primitive;

import suite.util.Fail;

public interface FltLngPredicate {

	public boolean test(float c, long f);

	public default FltLngPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
