package suite.primitive;

public class DblChr_ChrRethrow {

	public static DblChr_Chr fun2(DblChr_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
