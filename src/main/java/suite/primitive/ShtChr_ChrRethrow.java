package suite.primitive;

public class ShtChr_ChrRethrow {

	public static ShtChr_Chr fun2(ShtChr_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
