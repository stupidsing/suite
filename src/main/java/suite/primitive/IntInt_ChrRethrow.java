package suite.primitive;

public class IntInt_ChrRethrow {

	public static IntInt_Chr fun2(IntInt_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
