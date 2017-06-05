package suite.primitive;

public class LngFlt_ChrRethrow {

	public static LngFlt_Chr fun2(LngFlt_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
