package suite.primitive;

import suite.util.Fail;

public interface ChrIntPredicate {

	public boolean test(char c, int f);

	public default ChrIntPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
