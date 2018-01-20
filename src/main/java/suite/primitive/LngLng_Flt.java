package suite.primitive;

import suite.util.Fail;

public interface LngLng_Flt {

	public float apply(long c, long f);

	public default LngLng_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
