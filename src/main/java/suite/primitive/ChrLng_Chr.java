package suite.primitive;

import suite.util.Fail;

public interface ChrLng_Chr {

	public char apply(char c, long f);

	public default ChrLng_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
