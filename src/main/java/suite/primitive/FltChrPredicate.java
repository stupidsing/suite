package suite.primitive;

import suite.util.Fail;

public interface FltChrPredicate {

	public boolean test(float c, char f);

	public default FltChrPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
