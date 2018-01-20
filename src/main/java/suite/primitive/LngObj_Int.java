package suite.primitive;

import suite.util.Fail;

public interface LngObj_Int<T> {

	public int apply(long c, T t);

	public default LngObj_Int<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
