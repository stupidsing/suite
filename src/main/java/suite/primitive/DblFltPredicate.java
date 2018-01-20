package suite.primitive;

import suite.util.Fail;

public interface DblFltPredicate {

	public boolean test(double c, float f);

	public default DblFltPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
