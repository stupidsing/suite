package suite.primitive;

public class ChrInt_ShtRethrow {

	public static ChrInt_Sht fun2(ChrInt_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
