package suite.primitive;

public class IntFlt_ChrRethrow {

	public static IntFlt_Chr fun2(IntFlt_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
