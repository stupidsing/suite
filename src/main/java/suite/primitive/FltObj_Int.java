package suite.primitive;

import suite.util.Fail;

public interface FltObj_Int<T> {

	public int apply(float c, T t);

	public default FltObj_Int<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
