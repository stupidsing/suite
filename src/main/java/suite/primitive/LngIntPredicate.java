package suite.primitive;

import suite.util.Fail;

public interface LngIntPredicate {

	public boolean test(long c, int f);

	public default LngIntPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
