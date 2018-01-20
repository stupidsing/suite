package suite.primitive;

import suite.util.Fail;

public interface IntInt_Flt {

	public float apply(int c, int f);

	public default IntInt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return Fail.t("for " + c + ":" + f, ex);
			}
		};

	}
}
