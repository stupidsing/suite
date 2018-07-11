package suite.primitive; import static suite.util.Friends.fail;

public interface FltFlt_Dbl {

	public double apply(float c, float f);

	public default FltFlt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
