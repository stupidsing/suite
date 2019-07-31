package suite.primitive;

import static primal.statics.Fail.fail;

public interface FltInt_Obj<T> {

	public T apply(float c, int f);

	public default FltInt_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
