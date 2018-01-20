package suite.primitive;

import suite.util.Fail;

public interface LngFlt_Chr {

	public char apply(long c, float f);

	public default LngFlt_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
