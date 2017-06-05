package suite.primitive;

public class DblDbl_LngRethrow {

	public static DblDbl_Lng fun2(DblDbl_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
