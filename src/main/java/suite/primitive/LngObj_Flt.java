package suite.primitive;

import static primal.statics.Fail.fail;

public interface LngObj_Flt<T> {

	public float apply(long c, T t);

	public default LngObj_Flt<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return fail("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
