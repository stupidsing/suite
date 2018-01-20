package suite.primitive;

import suite.util.Fail;

public interface IntDbl_Lng {

	public long apply(int c, double f);

	public default IntDbl_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
