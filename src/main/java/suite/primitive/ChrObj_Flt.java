package suite.primitive;

import suite.util.Fail;

public interface ChrObj_Flt<T> {

	public float apply(char c, T t);

	public default ChrObj_Flt<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
