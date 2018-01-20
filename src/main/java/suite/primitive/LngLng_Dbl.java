package suite.primitive;

import suite.util.Fail;

public interface LngLng_Dbl {

	public double apply(long c, long f);

	public default LngLng_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
