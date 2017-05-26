package suite.primitive;

public class FltFlt_IntRethrow {

	public static FltFlt_Int fun2(FltFlt_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
