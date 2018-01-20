package suite.primitive;

import suite.util.Fail;

public interface ChrInt_Flt {

	public float apply(char c, int f);

	public default ChrInt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
