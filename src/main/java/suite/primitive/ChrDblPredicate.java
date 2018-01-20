package suite.primitive;

import suite.util.Fail;

public interface ChrDblPredicate {

	public boolean test(char c, double f);

	public default ChrDblPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
