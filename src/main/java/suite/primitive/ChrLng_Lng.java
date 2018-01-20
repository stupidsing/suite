package suite.primitive;

import suite.util.Fail;

public interface ChrLng_Lng {

	public long apply(char c, long f);

	public default ChrLng_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
