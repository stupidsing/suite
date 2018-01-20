package suite.primitive;

import suite.util.Fail;

public interface ChrDbl_Flt {

	public float apply(char c, double f);

	public default ChrDbl_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
