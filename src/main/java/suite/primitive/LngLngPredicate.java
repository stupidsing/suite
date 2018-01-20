package suite.primitive;

import suite.util.Fail;

public interface LngLngPredicate {

	public boolean test(long c, long f);

	public default LngLngPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
