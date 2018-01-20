package suite.primitive;

import suite.util.Fail;

public interface DblObj_Chr<T> {

	public char apply(double c, T t);

	public default DblObj_Chr<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
