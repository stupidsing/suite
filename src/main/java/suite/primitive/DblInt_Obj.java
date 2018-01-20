package suite.primitive;

import suite.util.Fail;

public interface DblInt_Obj<T> {

	public T apply(double c, int f);

	public default DblInt_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
