package suite.primitive;

import suite.util.Fail;

public interface LngObj_Flt<T> {

	public float apply(long c, T t);

	public default LngObj_Flt<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
