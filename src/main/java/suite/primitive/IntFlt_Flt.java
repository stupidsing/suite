package suite.primitive;

import suite.util.Fail;

public interface IntFlt_Flt {

	public float apply(int c, float f);

	public default IntFlt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
