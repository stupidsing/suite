package suite.primitive;

import suite.util.Fail;

public interface FltInt_Chr {

	public char apply(float c, int f);

	public default FltInt_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
