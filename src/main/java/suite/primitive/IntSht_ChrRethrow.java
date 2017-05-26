package suite.primitive;

public class IntSht_ChrRethrow {

	public static IntSht_Chr fun2(IntSht_Chr fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
