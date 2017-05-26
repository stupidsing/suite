package suite.primitive;

public class ShtSht_ChrRethrow {

	public static ShtSht_Chr fun2(ShtSht_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
