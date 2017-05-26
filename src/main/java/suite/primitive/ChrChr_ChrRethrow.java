package suite.primitive;

public class ChrChr_ChrRethrow {

	public static ChrChr_Chr fun2(ChrChr_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
