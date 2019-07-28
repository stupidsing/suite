package suite.primitive;

import static suite.util.Fail.fail;

public interface ChrObj_Lng<T> {

	public long apply(char c, T t);

	public default ChrObj_Lng<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return fail("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
