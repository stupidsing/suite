package suite.primitive;

public class IntLng_ChrRethrow {

	public static IntLng_Chr fun2(IntLng_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
