package suite.primitive;

import suite.util.Fail;

public interface FltDbl_Flt {

	public float apply(float c, double f);

	public default FltDbl_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
