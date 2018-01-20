package suite.primitive;

import suite.util.Fail;

public interface FltObj_Dbl<T> {

	public double apply(float c, T t);

	public default FltObj_Dbl<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
