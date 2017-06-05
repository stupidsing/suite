package suite.primitive;

public class ChrLng_ChrRethrow {

	public static ChrLng_Chr fun2(ChrLng_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
