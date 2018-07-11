package suite.primitive; import static suite.util.Friends.fail;

public interface ChrFlt_Dbl {

	public double apply(char c, float f);

	public default ChrFlt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
