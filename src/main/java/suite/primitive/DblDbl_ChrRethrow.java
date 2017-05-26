package suite.primitive;

public class DblDbl_ChrRethrow {

	public static DblDbl_Chr fun2(DblDbl_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
