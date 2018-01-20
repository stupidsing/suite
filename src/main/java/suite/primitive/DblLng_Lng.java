package suite.primitive;

import suite.util.Fail;

public interface DblLng_Lng {

	public long apply(double c, long f);

	public default DblLng_Lng rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
