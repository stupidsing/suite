package suite.primitive;

public class ShtFlt_IntRethrow {

	public static ShtFlt_Int fun2(ShtFlt_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
