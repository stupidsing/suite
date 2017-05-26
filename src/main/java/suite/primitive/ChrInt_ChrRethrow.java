package suite.primitive;

public class ChrInt_ChrRethrow {

	public static ChrInt_Chr fun2(ChrInt_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
