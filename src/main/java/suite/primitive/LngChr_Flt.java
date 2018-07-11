package suite.primitive; import static suite.util.Friends.fail;

public interface LngChr_Flt {

	public float apply(long c, char f);

	public default LngChr_Flt rethrow() {
		return (c, f) -> {
			try {
				return apply(c, f);
			} catch (Exception ex) {
				return fail("for " + c + ":" + f, ex);
			}
		};

	}
}
