package suite.primitive; import static suite.util.Friends.fail;

public interface FltFlt_Chr {

	public char apply(float c, float f);

	public default FltFlt_Chr rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
