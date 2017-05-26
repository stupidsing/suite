package suite.primitive;

public class IntSht_IntRethrow {

	public static IntSht_Int fun2(IntSht_Int fun) {
		return (c, f) -> {
			try {
				return fun.apply(c, f);
			} catch (Exception ex) {
				throw new RuntimeException("for " + c + ":" + f + ", ", ex);
			}
		};
	}

}
