package suite.primitive;

public class LngInt_IntRethrow {

	public static LngInt_Int fun2(LngInt_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
