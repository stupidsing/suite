package suite.primitive;

import static primal.statics.Fail.fail;

public interface ChrLng_Obj<T> {

	public T apply(char c, long f);

	public default ChrLng_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
