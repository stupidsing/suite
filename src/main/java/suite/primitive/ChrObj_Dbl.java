package suite.primitive;

import static primal.statics.Fail.fail;

public interface ChrObj_Dbl<T> {

	public double apply(char c, T t);

	public default ChrObj_Dbl<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return fail("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
