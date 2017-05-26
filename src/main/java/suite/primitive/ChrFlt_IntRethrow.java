package suite.primitive;

public class ChrFlt_IntRethrow {

	public static ChrFlt_Int fun2(ChrFlt_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
