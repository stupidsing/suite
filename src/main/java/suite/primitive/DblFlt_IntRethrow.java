package suite.primitive;

public class DblFlt_IntRethrow {

	public static DblFlt_Int fun2(DblFlt_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
