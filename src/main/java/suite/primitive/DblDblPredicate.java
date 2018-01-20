package suite.primitive;

import suite.util.Fail;

public interface DblDblPredicate {

	public boolean test(double c, double f);

	public default DblDblPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
