package suite.primitive;

public class ChrDbl_ChrRethrow {

	public static ChrDbl_Chr fun2(ChrDbl_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
