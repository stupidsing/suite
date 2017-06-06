package suite.primitive;

public interface ChrIntSink {

	public void sink2(char c, int f);

	public default ChrIntSink rethrow() {
		return (k, v) -> {
			try {
				sink2(k, v);
			} catch (Exception ex) {
				throw new RuntimeException("for key " + k, ex);
			}
		};
	}

}
