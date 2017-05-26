package suite.primitive;

public class ShtInt_ShtRethrow {

	public static ShtInt_Sht fun2(ShtInt_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
