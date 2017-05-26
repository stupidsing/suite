package suite.primitive;

public class FltDbl_IntRethrow {

	public static FltDbl_Int fun2(FltDbl_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
