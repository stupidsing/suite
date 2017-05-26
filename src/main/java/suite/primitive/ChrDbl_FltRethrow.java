package suite.primitive;

public class ChrDbl_FltRethrow {

	public static ChrDbl_Flt fun2(ChrDbl_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
