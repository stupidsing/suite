package suite.primitive;

public class ChrFlt_ChrRethrow {

	public static ChrFlt_Chr fun2(ChrFlt_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
