package suite.primitive;

import suite.util.Fail;

public interface DblFlt_Flt {

	public float apply(double c, float f);

	public default DblFlt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
