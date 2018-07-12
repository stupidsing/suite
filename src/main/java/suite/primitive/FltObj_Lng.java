package suite.primitive;

import static suite.util.Friends.fail;

public interface FltObj_Lng<T> {

	public long apply(float c, T t);

	public default FltObj_Lng<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return fail("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
