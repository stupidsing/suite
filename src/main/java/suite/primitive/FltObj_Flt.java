package suite.primitive;

import suite.util.Fail;

public interface FltObj_Flt<T> {

	public float apply(float c, T t);

	public default FltObj_Flt<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
