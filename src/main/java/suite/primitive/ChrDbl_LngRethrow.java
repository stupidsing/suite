package suite.primitive;

public class ChrDbl_LngRethrow {

	public static ChrDbl_Lng fun2(ChrDbl_Lng fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
