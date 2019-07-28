package suite.primitive;

import static suite.util.Fail.fail;

public interface DblChr_Obj<T> {

	public T apply(double c, char f);

	public default DblChr_Obj<T> rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
