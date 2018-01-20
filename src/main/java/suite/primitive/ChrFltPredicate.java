package suite.primitive;

import suite.util.Fail;

public interface ChrFltPredicate {

	public boolean test(char c, float f);

	public default ChrFltPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
