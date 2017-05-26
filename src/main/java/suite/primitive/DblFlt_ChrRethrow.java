package suite.primitive;

public class DblFlt_ChrRethrow {

	public static DblFlt_Chr fun2(DblFlt_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
