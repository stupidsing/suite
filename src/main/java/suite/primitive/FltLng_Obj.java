package suite.primitive;

import suite.util.Fail;

public interface FltLng_Obj<T> {

	public T apply(float c, long f);

	public default FltLng_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
