package suite.primitive;

import suite.util.Fail;

public interface LngLng_Chr {

	public char apply(long c, long f);

	public default LngLng_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
