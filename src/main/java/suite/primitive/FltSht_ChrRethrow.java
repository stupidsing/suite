package suite.primitive;

public class FltSht_ChrRethrow {

	public static FltSht_Chr fun2(FltSht_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
