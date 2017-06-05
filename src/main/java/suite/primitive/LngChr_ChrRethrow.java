package suite.primitive;

public class LngChr_ChrRethrow {

	public static LngChr_Chr fun2(LngChr_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
