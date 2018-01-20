package suite.primitive;

import suite.util.Fail;

public interface LngObj_Lng<T> {

	public long apply(long c, T t);

	public default LngObj_Lng<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
