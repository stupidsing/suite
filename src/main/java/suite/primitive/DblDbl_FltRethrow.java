package suite.primitive;

public class DblDbl_FltRethrow {

	public static DblDbl_Flt fun2(DblDbl_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
