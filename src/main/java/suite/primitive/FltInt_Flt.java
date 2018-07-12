package suite.primitive;

import static suite.util.Friends.fail;

public interface FltInt_Flt {

	public float apply(float c, int f);

	public default FltInt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
