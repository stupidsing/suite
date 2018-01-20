package suite.primitive;

import suite.util.Fail;

public interface IntDbl_Flt {

	public float apply(int c, double f);

	public default IntDbl_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
