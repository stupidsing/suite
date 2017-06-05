package suite.primitive;

public class LngFlt_IntRethrow {

	public static LngFlt_Int fun2(LngFlt_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
