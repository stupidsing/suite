package suite.primitive;

public class LngDbl_IntRethrow {

	public static LngDbl_Int fun2(LngDbl_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
