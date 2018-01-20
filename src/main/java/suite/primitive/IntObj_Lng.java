package suite.primitive;

import suite.util.Fail;

public interface IntObj_Lng<T> {

	public long apply(int c, T t);

	public default IntObj_Lng<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
