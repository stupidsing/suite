package suite.primitive;

public class FltFlt_ChrRethrow {

	public static FltFlt_Chr fun2(FltFlt_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
