package suite.primitive;

import suite.util.Fail;

public interface IntChr_Obj<T> {

	public T apply(int c, char f);

	public default IntChr_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
