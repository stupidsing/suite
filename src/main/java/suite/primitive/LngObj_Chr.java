package suite.primitive;

import suite.util.Fail;

public interface LngObj_Chr<T> {

	public char apply(long c, T t);

	public default LngObj_Chr<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
