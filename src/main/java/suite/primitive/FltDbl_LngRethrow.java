package suite.primitive;

public class FltDbl_LngRethrow {

	public static FltDbl_Lng fun2(FltDbl_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
