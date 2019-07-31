package suite.primitive;

import static primal.statics.Fail.fail;

public interface DblInt_Obj<T> {

	public T apply(double c, int f);

	public default DblInt_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
