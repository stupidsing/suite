package suite.primitive;

import suite.util.Fail;

public interface FltFlt_Dbl {

	public double apply(float c, float f);

	public default FltFlt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
