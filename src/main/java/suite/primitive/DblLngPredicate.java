package suite.primitive;

import suite.util.Fail;

public interface DblLngPredicate {

	public boolean test(double c, long f);

	public default DblLngPredicate rethrow() {
		return (c, f) -> {
			try {
				return test(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
