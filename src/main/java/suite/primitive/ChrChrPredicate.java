package suite.primitive;

import suite.util.Fail;

public interface ChrChrPredicate {

	public boolean test(char c, char f);

	public default ChrChrPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
