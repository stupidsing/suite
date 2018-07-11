package suite.primitive; import static suite.util.Friends.fail;

public interface IntFlt_Flt {

	public float apply(int c, float f);

	public default IntFlt_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
