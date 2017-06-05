package suite.primitive;

public class IntLng_IntRethrow {

	public static IntLng_Int fun2(IntLng_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
