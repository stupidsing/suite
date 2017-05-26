package suite.primitive;

public class IntSht_ShtRethrow {

	public static IntSht_Sht fun2(IntSht_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
