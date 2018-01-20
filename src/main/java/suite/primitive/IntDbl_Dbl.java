package suite.primitive;

import suite.util.Fail;

public interface IntDbl_Dbl {

	public double apply(int c, double f);

	public default IntDbl_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
