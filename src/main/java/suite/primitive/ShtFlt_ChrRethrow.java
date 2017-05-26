package suite.primitive;

public class ShtFlt_ChrRethrow {

	public static ShtFlt_Chr fun2(ShtFlt_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
