package suite.primitive;

import static suite.util.Friends.fail;

public interface IntObj_Chr<T> {

	public char apply(int c, T t);

	public default IntObj_Chr<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return fail("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
