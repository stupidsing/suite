package suite.primitive;

public class ShtInt_ChrRethrow {

	public static ShtInt_Chr fun2(ShtInt_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
