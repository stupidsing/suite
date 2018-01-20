package suite.primitive;

import suite.util.Fail;

public interface LngFltPredicate {

	public boolean test(long c, float f);

	public default LngFltPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
