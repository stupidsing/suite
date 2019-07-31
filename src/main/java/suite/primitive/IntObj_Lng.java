package suite.primitive;

import static primal.statics.Fail.fail;

public interface IntObj_Lng<T> {

	public long apply(int c, T t);

	public default IntObj_Lng<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return fail("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
