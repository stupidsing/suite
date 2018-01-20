package suite.primitive;

import suite.util.Fail;

public interface IntChrPredicate {

	public boolean test(int c, char f);

	public default IntChrPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
