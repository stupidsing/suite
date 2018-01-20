package suite.primitive;

import suite.util.Fail;

public interface IntFltPredicate {

	public boolean test(int c, float f);

	public default IntFltPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
