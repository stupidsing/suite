package suite.primitive;

public class FltLng_IntRethrow {

	public static FltLng_Int fun2(FltLng_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
