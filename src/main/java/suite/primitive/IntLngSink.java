package suite.primitive;

public interface IntLngSink {

	public void sink2(int c, long f);

	public default IntLngSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
