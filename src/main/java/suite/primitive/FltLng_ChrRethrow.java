package suite.primitive;

public class FltLng_ChrRethrow {

	public static FltLng_Chr fun2(FltLng_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
