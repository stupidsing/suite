package suite.primitive;

public class IntInt_IntRethrow {

	public static IntInt_Int fun2(IntInt_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
