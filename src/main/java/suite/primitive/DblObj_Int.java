package suite.primitive;

import static suite.util.Fail.fail;

public interface DblObj_Int<T> {

	public int apply(double c, T t);

	public default DblObj_Int<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return fail("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
