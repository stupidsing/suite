package suite.primitive;

public class IntChr_ChrRethrow {

	public static IntChr_Chr fun2(IntChr_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
