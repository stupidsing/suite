package suite.primitive;

public class FltInt_ShtRethrow {

	public static FltInt_Sht fun2(FltInt_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
