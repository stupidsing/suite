package suite.primitive;

import suite.util.Fail;

public interface ChrLngPredicate {

	public boolean test(char c, long f);

	public default ChrLngPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
