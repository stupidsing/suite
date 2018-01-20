package suite.primitive;

import suite.util.Fail;

public interface IntObj_Flt<T> {

	public float apply(int c, T t);

	public default IntObj_Flt<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
