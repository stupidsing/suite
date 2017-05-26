package suite.primitive;

public class IntInt_ShtRethrow {

	public static IntInt_Sht fun2(IntInt_Sht fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
