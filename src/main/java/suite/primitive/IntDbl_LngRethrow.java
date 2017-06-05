package suite.primitive;

public class IntDbl_LngRethrow {

	public static IntDbl_Lng fun2(IntDbl_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
