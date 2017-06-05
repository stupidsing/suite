package suite.primitive;

public class LngInt_ChrRethrow {

	public static LngInt_Chr fun2(LngInt_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
