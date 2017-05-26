package suite.primitive;

public class IntDbl_ChrRethrow {

	public static IntDbl_Chr fun2(IntDbl_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
