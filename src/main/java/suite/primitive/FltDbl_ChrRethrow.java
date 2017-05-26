package suite.primitive;

public class FltDbl_ChrRethrow {

	public static FltDbl_Chr fun2(FltDbl_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
