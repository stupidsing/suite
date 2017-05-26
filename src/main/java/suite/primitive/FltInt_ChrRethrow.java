package suite.primitive;

public class FltInt_ChrRethrow {

	public static FltInt_Chr fun2(FltInt_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
