package suite.primitive;

public class DblLng_IntRethrow {

	public static DblLng_Int fun2(DblLng_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
