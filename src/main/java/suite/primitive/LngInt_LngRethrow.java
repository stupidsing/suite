package suite.primitive;

public class LngInt_LngRethrow {

	public static LngInt_Lng fun2(LngInt_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
