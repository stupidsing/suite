package suite.primitive;

import suite.util.Fail;

public interface IntObj_Dbl<T> {

	public double apply(int c, T t);

	public default IntObj_Dbl<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
