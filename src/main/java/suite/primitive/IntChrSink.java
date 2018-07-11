package suite.primitive; import static suite.util.Friends.fail;

public interface IntChrSink {

	public void sink2(int c, char f);

	public default IntChrSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				fail("for key " + k, ex);
			}
		};
	}

}
