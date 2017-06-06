package suite.primitive;

public interface IntIntSink {

	public void sink2(int c, int f);

	public default IntIntSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
