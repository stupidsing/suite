package suite.primitive;

import suite.util.Fail;

public interface IntLngPredicate {

	public boolean test(int c, long f);

	public default IntLngPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
