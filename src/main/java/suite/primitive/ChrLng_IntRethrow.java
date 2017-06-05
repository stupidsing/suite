package suite.primitive;

public class ChrLng_IntRethrow {

	public static ChrLng_Int fun2(ChrLng_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
