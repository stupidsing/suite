package suite.primitive;

import static suite.util.Friends.fail;

public interface LngObj_Dbl<T> {

	public double apply(long c, T t);

	public default LngObj_Dbl<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return fail("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
