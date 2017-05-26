package suite.primitive;

public class FltChr_ChrRethrow {

	public static FltChr_Chr fun2(FltChr_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
