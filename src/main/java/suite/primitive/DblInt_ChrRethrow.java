package suite.primitive;

public class DblInt_ChrRethrow {

	public static DblInt_Chr fun2(DblInt_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
