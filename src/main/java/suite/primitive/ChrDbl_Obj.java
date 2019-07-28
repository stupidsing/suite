package suite.primitive;

import static suite.util.Fail.fail;

public interface ChrDbl_Obj<T> {

	public T apply(char c, double f);

	public default ChrDbl_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
