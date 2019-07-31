package suite.primitive;

import static primal.statics.Fail.fail;

public interface LngObj_Lng<T> {

	public long apply(long c, T t);

	public default LngObj_Lng<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return fail("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
