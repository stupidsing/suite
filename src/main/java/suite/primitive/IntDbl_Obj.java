package suite.primitive;

import static suite.util.Fail.fail;

public interface IntDbl_Obj<T> {

	public T apply(int c, double f);

	public default IntDbl_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
