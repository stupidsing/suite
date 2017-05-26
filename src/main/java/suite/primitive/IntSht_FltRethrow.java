package suite.primitive;

public class IntSht_FltRethrow {

	public static IntSht_Flt fun2(IntSht_Flt fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
