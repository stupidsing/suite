package suite.primitive;

public class IntDbl_IntRethrow {

	public static IntDbl_Int fun2(IntDbl_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
