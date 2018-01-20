package suite.primitive;

import suite.util.Fail;

public interface IntIntPredicate {

	public boolean test(int c, int f);

	public default IntIntPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
