package suite.primitive;

public class LngDbl_ChrRethrow {

	public static LngDbl_Chr fun2(LngDbl_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
