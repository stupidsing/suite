package suite.primitive;

import static primal.statics.Fail.fail;

public interface DblObj_Dbl<T> {

	public double apply(double c, T t);

	public default DblObj_Dbl<T> rethrow() {
		return (c, t) -> {
			try {
				return apply(c, t);
			} catch (Exception ex) {
				return fail("for " + c + ":" + t + ", ", ex);
			}
		};
	}

}
