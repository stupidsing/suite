package suite.primitive; import static suite.util.Friends.fail;

public interface FltInt_Int {

	public int apply(float c, int f);

	public default FltInt_Int rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
