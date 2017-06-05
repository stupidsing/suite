package suite.primitive;

public class LngFlt_LngRethrow {

	public static LngFlt_Lng fun2(LngFlt_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
