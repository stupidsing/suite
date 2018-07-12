package suite.primitive;

import static suite.util.Friends.fail;

public interface DblObj_Lng<T> {

	public long apply(double c, T t);

	public default DblObj_Lng<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return fail("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
