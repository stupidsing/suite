package suite.primitive;

public class LngDbl_LngRethrow {

	public static LngDbl_Lng fun2(LngDbl_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
