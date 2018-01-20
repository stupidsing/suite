package suite.primitive;

import suite.util.Fail;

public interface IntFlt_Chr {

	public char apply(int c, float f);

	public default IntFlt_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
