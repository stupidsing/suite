package suite.primitive;

import static suite.util.Friends.fail;

public interface FltObj_Int<T> {

	public int apply(float c, T t);

	public default FltObj_Int<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return fail("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
