package suite.primitive;

import suite.util.Fail;

public interface LngChrPredicate {

	public boolean test(long c, char f);

	public default LngChrPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
