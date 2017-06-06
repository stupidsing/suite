package suite.primitive;

public interface IntChrSink {

	public void sink2(int c, char f);

	public default IntChrSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
