package suite.primitive;

public class IntFlt_IntRethrow {

	public static IntFlt_Int fun2(IntFlt_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
