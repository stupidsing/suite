package suite.primitive;

import suite.util.Fail;

public interface DblIntPredicate {

	public boolean test(double c, int f);

	public default DblIntPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
