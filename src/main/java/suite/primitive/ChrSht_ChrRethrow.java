package suite.primitive;

public class ChrSht_ChrRethrow {

	public static ChrSht_Chr fun2(ChrSht_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
