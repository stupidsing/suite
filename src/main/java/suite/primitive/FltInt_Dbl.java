package suite.primitive; import static suite.util.Friends.fail;

public interface FltInt_Dbl {

	public double apply(float c, int f);

	public default FltInt_Dbl rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
