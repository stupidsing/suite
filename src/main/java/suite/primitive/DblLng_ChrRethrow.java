package suite.primitive;

public class DblLng_ChrRethrow {

	public static DblLng_Chr fun2(DblLng_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
