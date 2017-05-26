package suite.primitive;

public class IntChr_ShtRethrow {

	public static IntChr_Sht fun2(IntChr_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
