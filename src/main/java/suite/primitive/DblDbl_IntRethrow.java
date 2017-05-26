package suite.primitive;

public class DblDbl_IntRethrow {

	public static DblDbl_Int fun2(DblDbl_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
