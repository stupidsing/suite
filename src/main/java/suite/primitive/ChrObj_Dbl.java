package suite.primitive;

import suite.util.Fail;

public interface ChrObj_Dbl<T> {

	public double apply(char c, T t);

	public default ChrObj_Dbl<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
