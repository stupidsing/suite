package suite.primitive;

public interface IntDblSink {

	public void sink2(int c, double f);

	public default IntDblSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
