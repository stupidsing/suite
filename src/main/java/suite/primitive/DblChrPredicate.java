package suite.primitive;

import suite.util.Fail;

public interface DblChrPredicate {

	public boolean test(double c, char f);

	public default DblChrPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
