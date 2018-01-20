package suite.primitive;

import suite.util.Fail;

public interface ChrFlt_Obj<T> {

	public T apply(char c, float f);

	public default ChrFlt_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
